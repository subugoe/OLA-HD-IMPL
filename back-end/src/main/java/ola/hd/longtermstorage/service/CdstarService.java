package ola.hd.longtermstorage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import ola.hd.longtermstorage.component.MutexFactory;
import ola.hd.longtermstorage.domain.SearchRequest;
import ola.hd.longtermstorage.domain.SearchResults;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CdstarService implements ArchiveManagerService, SearchService {

    @Value("${cdstar.url}")
    private String url;

    @Value("${cdstar.username}")
    private String username;

    @Value("${cdstar.password}")
    private String password;

    @Value("${cdstar.vault}")
    private String vault;

    @Value("${cdstar.offlineProfile}")
    private String offlineProfile;

    @Value("${cdstar.onlineProfile}")
    private String onlineProfile;

    @Value("${cdstar.mirrorProfile}")
    private String mirrorProfile;

    @Value("${offline.mimeTypes}")
    private String offlineMimeTypes;

    private final MutexFactory<String> mutexFactory;

    @Autowired
    public CdstarService(MutexFactory<String> mutexFactory) {
        this.mutexFactory = mutexFactory;
    }

    @Override
    public List<AbstractMap.SimpleImmutableEntry<String, String>> importZipFile(Path extractedDir,
                                                                                String pid,
                                                                                List<AbstractMap.SimpleImmutableEntry<String, String>> metaData) throws IOException {

        String txId = null;

        try {
            // Get the transaction ID
            txId = getTransactionId();

            String onlineArchiveId = createArchive(txId, false);
            String offlineArchiveId = createArchive(txId, true);

            uploadData(extractedDir, txId, onlineArchiveId, offlineArchiveId);

            // Update archive meta-data
            setArchiveMetaData(onlineArchiveId, metaData, pid, txId, null, null);
            setArchiveMetaData(offlineArchiveId, metaData, pid, txId, null, null);

            // Commit the transaction
            commitTransaction(txId);

            // Meta-data to return
            List<AbstractMap.SimpleImmutableEntry<String, String>> results = new ArrayList<>();
            results.add(new AbstractMap.SimpleImmutableEntry<>("ONLINE-URL", url + vault + "/" + onlineArchiveId + "?with=files,meta"));
            results.add(new AbstractMap.SimpleImmutableEntry<>("OFFLINE-URL", url + vault + "/" + offlineArchiveId + "?with=files,meta"));

            return results;
        } catch (Exception ex) {
            if (txId != null) {
                rollbackTransaction(txId);
            }

            throw ex;
        }
    }

    @Override
    public List<AbstractMap.SimpleImmutableEntry<String, String>> importZipFile(Path extractedDir, String pid,
                                                                                List<AbstractMap.SimpleImmutableEntry<String, String>> metaData,
                                                                                String prevPid) throws IOException {

        String txId = null;

        try {
            // Get the online archive of the previous version
            String prevOnlineArchiveId = getArchiveIdFromIdentifier(prevPid, onlineProfile);

            // Get the transaction ID
            txId = getTransactionId();

            String onlineArchiveId = createArchive(txId, false);
            String offlineArchiveId = createArchive(txId, true);

            uploadData(extractedDir, txId, onlineArchiveId, offlineArchiveId);

            // Update archive meta-data of current version
            setArchiveMetaData(onlineArchiveId, metaData, pid, txId, prevPid, null);
            setArchiveMetaData(offlineArchiveId, metaData, pid, txId, prevPid, null);

            // Update archive meta-data of previous version. Update the online archive only because it's not possible
            // to update meta-data of an offline archive
            linkToNextVersion(prevOnlineArchiveId, txId, pid);

            // Commit the transaction
            commitTransaction(txId);

            // Meta-data to return
            List<AbstractMap.SimpleImmutableEntry<String, String>> results = new ArrayList<>();
            results.add(new AbstractMap.SimpleImmutableEntry<>("ONLINE-URL", url + vault + "/" + onlineArchiveId + "?with=files,meta"));
            results.add(new AbstractMap.SimpleImmutableEntry<>("OFFLINE-URL", url + vault + "/" + offlineArchiveId + "?with=files,meta"));

            return results;

        } catch (IOException ex) {
            if (txId != null) {
                rollbackTransaction(txId);
            }

            throw ex;
        }
    }

    private String getTransactionId() throws IOException {

        String transactionUrl = url + "_tx/";

        OkHttpClient client = new OkHttpClient();

        RequestBody txBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("timeout", "300")
                .build();

        Request request = new Request.Builder()
                .url(transactionUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .post(txBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    String bodyString = response.body().string();

                    // Parse the returned JSON
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(bodyString);
                    return root.get("id").asText();
                }
            }

            // Cannot get the transaction ID? Abort!
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot start the upload transaction.");
        }
    }

    private void uploadData(Path extractedDir, String txId, String onlineArchiveId, String offlineArchiveId) throws IOException {

        String onlineBaseUrl = url + vault + "/" + onlineArchiveId;
        String offlineBaseUrl = url + vault + "/" + offlineArchiveId;

        List<String> offlineTypes = Arrays.asList(offlineMimeTypes.split(";"));
        Tika tika = new Tika();

        Files.walk(extractedDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String onlineUrl = onlineBaseUrl + "/" +
                            URLEncoder.encode(extractedDir.relativize(path).toString(), StandardCharsets.UTF_8);
                    String offlineUrl = offlineBaseUrl + "/" +
                            URLEncoder.encode(extractedDir.relativize(path).toString(), StandardCharsets.UTF_8);

                    String mimeType;
                    try {

                        // Try to figure out the correct MIME type
                        mimeType = tika.detect(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    // If the MIME type is unrecognizable
                    if (mimeType == null || mimeType.isEmpty()) {
                        mimeType = "application/octet-stream";
                    }

                    File file = path.toFile();

                    try {
                        // Offline file?
                        if (offlineTypes.contains(mimeType)) {

                            // Only send to offline archive
                            sendRquest(offlineUrl, txId, file, mimeType);
                        } else {

                            // For other files, send to both archives
                            sendRquest(offlineUrl, txId, file, mimeType);
                            sendRquest(onlineUrl, txId, file, mimeType);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void sendRquest(String url, String txId, File file, String mimeType) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Content-Type", mimeType)
                .addHeader("X-Transaction", txId)
                .put(RequestBody.create(MediaType.parse(mimeType), file))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {

                // Something is wrong, throw the exception
                throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot send data to CDSTAR. URL: " + url);
            }
        }
    }

    private void commitTransaction(String txId) throws IOException {

        String txUrl = url + "_tx/" + txId;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(txUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .post(RequestBody.create(null, ""))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {

                // Something is wrong here
                throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot commit the transaction");
            }
        }
    }

    private void rollbackTransaction(String txId) throws IOException {

        String txUrl = url + "_tx/" + txId;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(txUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .delete()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {

                // Something is wrong here
                throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Error when rolling back the transaction");
            }
        }
    }

    private String createArchive(String txId, boolean isOffline) throws IOException {
        String fullUrl = url + vault;
        String profile = onlineProfile;

        if (isOffline) {
            profile = offlineProfile;
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("profile", profile)
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("X-Transaction", txId)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String location = response.header("Location");

                // Extract the archive ID from the location string
                String archive = "";
                if (location != null) {
                    int lastSlashIndex = location.lastIndexOf('/');
                    archive = location.substring(lastSlashIndex + 1);
                }

                return archive;
            }

            // Something is wrong, throw the exception
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot create archive");
        }
    }

    private void setArchiveMetaData(String archiveId, List<AbstractMap.SimpleImmutableEntry<String, String>> metaData,
                                    String pid, String txId, String prevPid, List<String> nextVersions) throws IOException {
        String fullUrl = url + vault + "/" + archiveId;
        OkHttpClient client = new OkHttpClient();

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // Extract proper meta-data from bagit-info.txt
        if (metaData != null) {
            for (AbstractMap.SimpleImmutableEntry<String, String> item : metaData) {
                String key = item.getKey();
                switch (key) {
                    case "Bagging-Date":
                        builder.addFormDataPart("meta:dc:date", item.getValue());
                        break;
                    case "Ocrd-Identifier":
                        builder.addFormDataPart("meta:dc:identifier", item.getValue());
                }
            }
        }

        // Set identifier
        if (pid != null) {
            builder.addFormDataPart("meta:dc:identifier", pid);
        }

        // Link with other versions if necessary
        if (prevPid != null) {
            builder.addFormDataPart("meta:dc:source", prevPid);
        }
        if (nextVersions != null) {
            for (String nextVersion : nextVersions) {
                builder.addFormDataPart("meta:dc:relation", nextVersion);
            }
        }

        RequestBody requestBody = builder.build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("X-Transaction", txId)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {

                // Something is wrong, throw the exception
                throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot set archive meta-data");
            }
        }
    }

    private void linkToNextVersion(String archiveId, String txId, String nextPid) throws IOException {

        // Execute sequentially if it tries to append to the same archive
        synchronized (mutexFactory.getMutex(archiveId)) {

            // Get the list of current next version (meta:dc:relation)
            List<String> nextVersions = getCurrentNextVersions(archiveId, txId);

            // Add another next version
            nextVersions.add(nextPid);

            setArchiveMetaData(archiveId, null, null, txId, null, nextVersions);
        }
    }

    private List<String> getCurrentNextVersions(String archiveId, String txId) throws IOException {

        List<String> results = new ArrayList<>();
        String fullUrl = url + vault + "/" + archiveId + "?meta";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("X-Transaction", txId)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {

                    // Parse the returned JSON
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.body().string());

                    // Get the array of the dc:relation
                    JsonNode relations = root.get("dc:relation");

                    // There is no relation, return empty list
                    if (relations == null) {
                        return results;
                    }

                    for (JsonNode item : relations) {
                        results.add(item.asText());
                    }
                    return results;
                }
            }

            // Cannot get the archive meta-data? Throw the exception
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()),
                    "Error when getting the archive meta-data with the identifier " + archiveId);
        }
    }

    @Override
    public Response export(String identifier, String type) throws IOException {

        String archiveId;

        // Quick export?
        if (type.equals("quick")) {
            archiveId = getArchiveIdFromIdentifier(identifier, onlineProfile);
        } else {
            // Full export
            archiveId = getArchiveIdFromIdentifier(identifier, mirrorProfile);
        }

        // Archive not found
        if (archiveId.equals("NOT_FOUND")) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Archive not found.");
        }

        // The archive can't be exported because it is still on tape
        if (!isArchiveOpen(archiveId)) {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, "The archive is still on tape. Please make a full export request first.");
        }

        return exportArchive(archiveId);
    }

    @Override
    public void downloadFiles(String archiveId, String[] paths, OutputStream outputStream) throws IOException {
        String baseUrl = url + vault + "/" + archiveId;

        OkHttpClient client = new OkHttpClient();

        Request.Builder requestBuilder = new Request.Builder()
                .addHeader("Authorization", Credentials.basic(username, password))
                .get();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

            for (String path : paths) {
                String fullUrl = baseUrl + "/" + path;
                Request request = requestBuilder.url(fullUrl).build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {

                        try (InputStream inputStream = response.body().byteStream()) {
                            String fileName = path.substring(path.lastIndexOf('/') + 1);
                            ZipEntry zipEntry = new ZipEntry(fileName);
                            zipOutputStream.putNextEntry(zipEntry);

                            byte[] bytes = new byte[1024];
                            int length;
                            while ((length = inputStream.read(bytes)) != -1) {
                                zipOutputStream.write(bytes, 0, length);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void moveFromTapeToDisk(String identifier) throws IOException {

        // Get cold-archive ID from the PID/PPN
        String archiveId = getArchiveIdFromIdentifier(identifier, offlineProfile);

        // Change the profile of the archive to a mirror profile
        if (!archiveId.equals("NOT_FOUND")) {
            updateProfile(archiveId, mirrorProfile);
        }
        throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Archive not found.");
    }

    @Override
    public void moveFromDiskToTape(String identifier) throws IOException {

        // Get mirror-archive ID from the PID/PPN
        String archiveId = getArchiveIdFromIdentifier(identifier, mirrorProfile);

        // Change the profile of the archive to a cold profile
        if (!archiveId.equals("NOT_FOUND")) {
            updateProfile(archiveId, offlineProfile);
        }
        throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Archive not found.");
    }

    private void updateProfile(String archiveId, String newProfile) throws IOException {
        String fullUrl = url + vault + "/" + archiveId;
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("profile", newProfile)
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {

                // Something is wrong, throw the exception
                throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot update archive profile");
            }
        }
    }

    private String getArchiveIdFromIdentifier(String identifier, String profile) throws IOException {
        String fullUrl = url + vault;

        // Search for archive with specified identifier (PPN, PID)
        String query = String.format("dcIdentifier:\"%s\" AND profile:%s", identifier, profile);

        // Sort by modified time in descending order
        String order = "-modified";

        // Number of the returned results
        String limit = "1";

        // Construct the URL
        HttpUrl httpUrl = HttpUrl.parse(fullUrl).newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("order", order)
                .addQueryParameter("limit", limit)
                .build();

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(httpUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    String bodyString = response.body().string();

                    // Parse the returned JSON
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(bodyString);
                    JsonNode hits = root.get("hits");
                    JsonNode firstElement = hits.get(0);

                    // Archive found
                    if (firstElement != null) {
                        return firstElement.get("id").asText();
                    }

                    // Archive not found
                    return "NOT_FOUND";
                }
            }

            // Cannot get the archive ID? Throw the exception
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Error when getting the archive with the identifier " + identifier);
        }
    }

    private Response exportArchive(String archiveId) throws IOException {
        String fullUrl = url + vault + "/" + archiveId;

        // Construct the URL
        HttpUrl httpUrl = HttpUrl.parse(fullUrl).newBuilder()
                .addQueryParameter("export", "zip")
                .build();

        Request request = new Request.Builder()
                .url(httpUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .get()
                .build();

        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            return response;
        }

        // Cannot export the archive? Throw the exception
        throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot export the archive " + archiveId);
    }

    private boolean isArchiveOpen(String archiveId) throws IOException {
        String fullUrl = url + vault + "/" + archiveId;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    String bodyString = response.body().string();

                    // Parse the returned JSON
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(bodyString);
                    String state = root.get("state").asText();

                    // Open-state archive
                    return state.equals("open") || state.equals("locked");

                }
            }

            // Cannot get the archive state? Throw the exception
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Error when getting the archive state.");
        }
    }

    @Override
    public SearchResults search(SearchRequest searchRequest) throws IOException {
        String fullUrl = url + vault;

        // Only search on archives on hard drive and on the latest version
        // Search on hard drive by default because data on tapes are not indexed (upload and immediately close the archive)
        String query = String.format("(%s) AND NOT _exists_:dcRelation", searchRequest.getQuery());

        // Construct the URL
        HttpUrl httpUrl = HttpUrl.parse(fullUrl).newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("limit", searchRequest.getLimit() + "")
                .build();

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(httpUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    String bodyString = response.body().string();

                    // Parse the returned JSON
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(bodyString, SearchResults.class);
                }
            }

            // Cannot search? Throw exception
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Error when performing search.");
        }
    }

    @Override
    public boolean isArchiveOnDisk(String identifier) throws IOException {

        String archiveId = getArchiveIdFromIdentifier(identifier, mirrorProfile);

        if (archiveId.equals("NOT_FOUND")) {
            return false;
        }

        return isArchiveOpen(archiveId);
    }

    @Override
    public String getArchiveInfo(String id, boolean withFile) throws IOException {
        String fullUrl = url + vault + "/" + id + "?with=meta";

        if (withFile) {
            fullUrl += ",files";
        }

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    return response.body().string();
                }
            }

            if (response.code() == HttpStatus.NOT_FOUND.value()) {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "Archive not found.");
            }

            // Cannot search? Throw exception
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Error when getting archive info.");
        }
    }
}
