package ola.hd.longtermstorage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import ola.hd.longtermstorage.exception.ImportException;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CdstarService implements ImportService, ExportService {

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

    @Value("${offline.mimeTypes}")
    private String offlineMimeTypes;

    private final PidService pidService;

    @Autowired
    public CdstarService(PidService pidService) {
        this.pidService = pidService;
    }

    @Override
    public void importZipFile(Path extractedDir, String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> metaData) throws IOException, ImportException {

        String txId = null;
        String onlineArchiveId, offlineArchiveId;

        long start = System.currentTimeMillis();
        try {
            // Get the transaction ID
            txId = getTransactionId();

            onlineArchiveId = createArchive(txId, false);
            offlineArchiveId = createArchive(txId, true);

            uploadData(extractedDir, txId, onlineArchiveId, offlineArchiveId);

            // Update archive meta-data
            setArchiveMetaData(onlineArchiveId, metaData, pid, txId, null, null);
            setArchiveMetaData(offlineArchiveId, metaData, pid, txId, null, null);

            // Commit the transaction
            commitTransaction(txId);

        } catch (Exception e) {
            if (txId != null) {
                rollbackTransaction(txId);
            }
            throw e;
        }

        // Update data for the PID
        List<AbstractMap.SimpleImmutableEntry<String, String>> pidData = new ArrayList<>();

        // Update the online and offline URL
        pidData.add(new AbstractMap.SimpleImmutableEntry<>("ONLINE-URL", url + vault + "/" + onlineArchiveId + "?with=files,meta"));
        pidData.add(new AbstractMap.SimpleImmutableEntry<>("OFFLINE-URL", url + vault + "/" + offlineArchiveId + "?with=files,meta"));

        // Keep all meta-data from the bag-info.txt
        pidData.addAll(metaData);

        // Update the PID
        pidService.updatePid(pid, pidData);

        long end = System.currentTimeMillis();
        long elapsed = (end - start) / 1000;
        System.out.println("Upload time: " + elapsed + " seconds");
    }

    @Override
    public void importZipFile(Path extractedDir, String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> metaData, String prevPid) throws IOException, ImportException {
        String txId = null;
        String onlineArchiveId, offlineArchiveId;

        // Get the online archive of the previous version
        String prevOnlineArchiveId = getArchiveIdFromIdentifier(prevPid, onlineProfile);

        long start = System.currentTimeMillis();
        try {
            // Get the transaction ID
            txId = getTransactionId();

            onlineArchiveId = createArchive(txId, false);
            offlineArchiveId = createArchive(txId, true);

            uploadData(extractedDir, txId, onlineArchiveId, offlineArchiveId);

            // Update archive meta-data of current version
            setArchiveMetaData(onlineArchiveId, metaData, pid, txId, prevPid, null);
            setArchiveMetaData(offlineArchiveId, metaData, pid, txId, prevPid, null);

            // Update archive meta-data of previous version. Update the online archive only because it's not possible
            // to update meta-data of an offline archive
            linkToNextVersion(prevOnlineArchiveId, txId, pid);

            // Commit the transaction
            commitTransaction(txId);

        } catch (Exception e) {
            if (txId != null) {
                rollbackTransaction(txId);
            }
            throw e;
        }

        // Update data for the PID
        List<AbstractMap.SimpleImmutableEntry<String, String>> pidData = new ArrayList<>();

        // Update the online and offline URL
        pidData.add(new AbstractMap.SimpleImmutableEntry<>("ONLINE-URL", url + vault + "/" + onlineArchiveId + "?with=files,meta"));
        pidData.add(new AbstractMap.SimpleImmutableEntry<>("OFFLINE-URL", url + vault + "/" + offlineArchiveId + "?with=files,meta"));
        pidData.add(new AbstractMap.SimpleImmutableEntry<>("PREVIOUS-VERSION", prevPid));

        // Keep all meta-data from the bag-info.txt
        pidData.addAll(metaData);

        // Update the PID of the new ZIP
        pidService.updatePid(pid, pidData);

        // Update the old PID to link to the new version
        List<AbstractMap.SimpleImmutableEntry<String, String>> pidAppendedData = new ArrayList<>();
        pidAppendedData.add(new AbstractMap.SimpleImmutableEntry<>("NEXT-VERSION", pid));
        pidService.appendData(prevPid, pidAppendedData);

        long end = System.currentTimeMillis();
        long elapsed = (end - start) / 1000;
        System.out.println("Upload time: " + elapsed + " seconds");
    }

    private String getTransactionId() throws ImportException, IOException {

        String transactionUrl = url + "_tx/";

        OkHttpClient client = new OkHttpClient();

        RequestBody txBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("timeout", "60")
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
            ImportException exception = new ImportException("Cannot start the upload transaction");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
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
                        //mimeType = Files.probeContentType(path);
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
                    } catch (ImportException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void sendRquest(String url, String txId, File file, String mimeType) throws IOException, ImportException {
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
                ImportException exception = new ImportException("Cannot send data to CDSTAR. URL: " + url);
                exception.setHttpStatusCode(response.code());
                exception.setHttpMessage(response.message());

                throw exception;
            }
        }
    }

    private void commitTransaction(String txId) throws IOException, ImportException {

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
                ImportException exception = new ImportException("Cannot commit the transaction");
                exception.setHttpStatusCode(response.code());
                exception.setHttpMessage(response.message());

                throw exception;
            }
        }
    }

    private void rollbackTransaction(String txId) throws IOException, ImportException {

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
                ImportException exception = new ImportException("Error when rolling back the transaction");
                exception.setHttpStatusCode(response.code());
                exception.setHttpMessage(response.message());

                throw exception;
            }
        }
    }

    private String createArchive(String txId, boolean isOffline) throws IOException, ImportException {
        String fullUrl = url + vault;

        OkHttpClient client = new OkHttpClient();

        // To create an online archive, just send an empty body
        RequestBody requestBody = RequestBody.create(null, "");

        // To createa an offline archive, set an appropriate profile for it
        if (isOffline) {
            requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("profile", offlineProfile)
                    .build();
        }

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
            ImportException exception = new ImportException("Cannot create archive");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
        }
    }

    private void setArchiveMetaData(String archiveId, List<AbstractMap.SimpleImmutableEntry<String, String>> metaData,
                                    String pid, String txId, String prevPid, String nextPid) throws IOException, ImportException {
        String fullUrl = url + vault + "/" + archiveId;
        OkHttpClient client = new OkHttpClient();

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // Extract proper meta-data
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
        if (nextPid != null) {
            builder.addFormDataPart("meta:dc:relation", nextPid);
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
                ImportException exception = new ImportException("Cannot set archive meta-data");
                exception.setHttpStatusCode(response.code());
                exception.setHttpMessage(response.message());

                throw exception;
            }
        }
    }

    private void linkToNextVersion(String archiveId, String txId, String nextPid) throws IOException, ImportException {
        setArchiveMetaData(archiveId, null, null, txId, null, nextPid);
    }

    @Override
    public byte[] export(String identifier) throws IOException, ImportException {
        String archiveId = getArchiveIdFromIdentifier(identifier, onlineProfile);
        return exportArchive(archiveId);
    }

    private String getArchiveIdFromIdentifier(String identifier, String profile) throws IOException, ImportException {
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
                    throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "The archive with identifier " + identifier + " could not be found.");
                }
            }

            // Cannot get the archive ID? Throw the exception
            ImportException exception = new ImportException("Cannot get the archive from its identifier");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
        }
    }

    private byte[] exportArchive(String archiveId) throws IOException, ImportException {
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
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    return response.body().bytes();
                }
            }

            // Cannot get the archive ID? Throw the exception
            ImportException exception = new ImportException("Cannot export archive");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
        }
    }
}
