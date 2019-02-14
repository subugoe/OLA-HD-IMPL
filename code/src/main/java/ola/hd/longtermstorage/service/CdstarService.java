package ola.hd.longtermstorage.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import ola.hd.longtermstorage.domain.ImportResult;
import ola.hd.longtermstorage.exception.ImportException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Service
public class CdstarService implements ImportService {

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

    @Value("${offline.mimeTypes}")
    private String offlineMimeTypes;

    public ImportResult importZipFile(File file, Path extractedDir) throws Exception {

        ImportResult importResult;
        String txId = null;

        try {
            // Get the transaction ID
            txId = getTransactionId();

            final String finalTxId = txId;

            String onlineArchiveId = createArchive(txId, false);
            String offlineArchiveId = createArchive(txId, true);

            // Upload online data
            String ppn = getPPN(extractedDir);
            System.out.println("PPN: " + ppn);

            long start = System.currentTimeMillis();
            uploadData(extractedDir, finalTxId, onlineArchiveId, offlineArchiveId);
            long end = System.currentTimeMillis();
            long elapsed = (end - start) / 1000;

            // TODO: Send PPN to the indexer

            // Commit the transaction
            commitTransaction(txId);

            System.out.println("Upload time: " + elapsed + " seconds");

            System.out.println("Online archive: " + onlineArchiveId);
            System.out.println("Offline archive: " + offlineArchiveId);

            importResult = new ImportResult();
            importResult.add("ONLINE_URL", url + vault + "/" + onlineArchiveId + "?with=files,meta");
            importResult.add("OFFLINE_URL", url + vault + "/" + offlineArchiveId + "?with=files,meta");
            if (!ppn.isEmpty()) {
                importResult.add("PPN", ppn);
            }

        } catch (Exception e) {
            if (txId != null) {
                rollbackTransaction(txId);
            }
            throw e;
        }

        return importResult;
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
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(bodyString).getAsJsonObject();

                    return jsonObject.getAsJsonPrimitive("id").getAsString();
                }
            }

            // Cannot get the transaction ID? Abort!
            ImportException exception = new ImportException("Cannot start the upload transaction");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
        }
    }

    private String getPPN(Path extractedDir) throws IOException {

        // Find the PPN
        Path ppnPath = Files.walk(extractedDir)
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().startsWith("PPN"))
                .findFirst()
                .orElse(null);

        String ppn = "";
        if (ppnPath != null) {
            File ppnFile = ppnPath.toFile();
            ppn = FilenameUtils.getBaseName(ppnFile.getName());
        }
        return ppn;
    }

    private void uploadData(Path extractedDir, String txId, String onlineArchiveId, String offlineArchiveId) throws IOException {

        String onlineBaseUrl = url + vault + "/" + onlineArchiveId;
        String offlineBaseUrl = url + vault + "/" + offlineArchiveId;

        List<String> offlineTypes = Arrays.asList(offlineMimeTypes.split(";"));

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
                        mimeType = Files.probeContentType(path);
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
                return getArchiveId(location);
            }

            // Something is wrong, throw the exception
            ImportException exception = new ImportException("Cannot create archive");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
        }
    }

    private String getArchiveId(String location) {
        String archive = "";
        if (location != null) {
            int lastSlashIndex = location.lastIndexOf('/');
            archive = location.substring(lastSlashIndex + 1);
        }

        return archive;
    }
}
