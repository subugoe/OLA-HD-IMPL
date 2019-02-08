package ola.hd.longtermstorage.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import ola.hd.longtermstorage.domain.ImportResult;
import ola.hd.longtermstorage.exception.ImportException;
import ola.hd.longtermstorage.helper.Operation;
import ola.hd.longtermstorage.helper.OperationHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

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

    @Value("${offline.fileTypes}")
    private String offlineFileTypes;

    private static final MediaType MEDIA_TYPE_ZIP = MediaType.parse("application/zip");

    public ImportResult importZipFile(File file, Path extractedDir) throws Exception {

        // TODO: Extract meta-data

        ImportResult importResult;
        String txId = null;

        try {
            // Get the transaction ID
            txId = OperationHelper.doWithRetry(3, new Operation() {
                @Override
                public String run() throws IOException, ImportException {
                    return getTransactionId();
                }
            });

            final String finalTxId = txId;

            // Upload online data
            String onlineArchive = uploadOnlineData(extractedDir, finalTxId);
            System.out.println("Online archive: " + onlineArchive);

            // Upload offline data
            String offlineArchive = OperationHelper.doWithRetry(3, new Operation() {
                @Override
                public String run() throws IOException, ImportException {
                    return uploadOfflineData(file, finalTxId);
                }
            });
            System.out.println("Offline archive: " + offlineArchive);

            // Commit the transaction
            commitTransaction(txId);

            importResult = new ImportResult();
            importResult.add("online_url", onlineArchive);
            importResult.add("offline_url", offlineArchive);

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

    private String uploadOnlineData(Path extractedDir, String txId) throws IOException, ImportException {

        // Exclude tif files (*.tiff, *.tif)
//        String fullUrl = url + vault + "?exclude=" + offlineFileTypes;
//
//        OkHttpClient client = new OkHttpClient.Builder()
//                .readTimeout(5, TimeUnit.MINUTES)
//                .writeTimeout(2, TimeUnit.MINUTES)
//                .build();
//
//        Request request = new Request.Builder()
//                .url(fullUrl)
//                .addHeader("Authorization", Credentials.basic(username, password))
//                .addHeader("Content-Type", "application/zip")
//                .addHeader("X-Transaction", txId)
//                .post(RequestBody.create(MEDIA_TYPE_ZIP, file))
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (response.isSuccessful()) {
//                String location = response.header("Location");
//                String archiveId = getArchiveId(location);
//                return url + vault + archiveId;
//            }
//
//            // Something is wrong, throw the exception
//            ImportException exception = new ImportException("Cannot upload online data");
//            exception.setHttpStatusCode(response.code());
//            exception.setHttpMessage(response.message());
//
//            throw exception;
//        }

        String archiveId = createArchive(txId);

        String fullUrl = url + vault + "/" + archiveId;
        OkHttpClient client = new OkHttpClient();

        Files.walk(extractedDir)
                .filter(path -> Files.isRegularFile(path) && !path.toString().endsWith(".tif"))
                .forEach(path -> {
                    String finalUrl = fullUrl + "/" + URLEncoder.encode(extractedDir.relativize(path).toString(), StandardCharsets.UTF_8);

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

                    // TODO: Get the PPN
                    //  Upload data to the tape

                    Request request = new Request.Builder()
                            .url(finalUrl)
                            .addHeader("Authorization", Credentials.basic(username, password))
                            .addHeader("Content-Type", mimeType)
                            .addHeader("X-Transaction", txId)
                            .put(RequestBody.create(MediaType.parse(mimeType), file))
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {

                            // Something is wrong, throw the exception
                            ImportException exception = new ImportException("Cannot upload online data");
                            exception.setHttpStatusCode(response.code());
                            exception.setHttpMessage(response.message());

                            throw exception;
                        }
                    } catch (IOException | ImportException e) {
                        throw new RuntimeException(e);
                    }
                });

        return fullUrl;
    }

    private String uploadOfflineData(File file, String txId) throws IOException, ImportException {

        String fullUrl = url + vault;

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("/" + file.getName(), file.getName(),
                        RequestBody.create(MEDIA_TYPE_ZIP, file))
                .addFormDataPart("profile", offlineProfile)
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Content-Type", "application/zip")
                .addHeader("X-Transaction", txId)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String location = response.header("Location");
                String archiveId = getArchiveId(location);
                return url + vault + "/" + archiveId;
            }

            // Something is wrong, throw the exception
            ImportException exception = new ImportException("Cannot upload offline data");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
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

    private String createArchive(String txId) throws IOException, ImportException {
        String fullUrl = url + vault;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("X-Transaction", txId)
                .post(RequestBody.create(null, ""))
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
