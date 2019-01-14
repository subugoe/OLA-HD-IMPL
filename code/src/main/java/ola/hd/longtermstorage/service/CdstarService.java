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

    public ImportResult importZipFile(File file) throws Exception {

        // TODO: Extract meta-data

        ImportResult importResult = null;
        final String[] txId = {null};

        try {
            // Get the transaction ID
            OperationHelper.doWithRetry(3, new Operation() {
                @Override
                public void run() throws IOException, ImportException {
                    txId[0] = getTransactionId();
                }
            });

            // Create archives for online and offline data
            String onlineArchive = createArchive("default", txId[0]);
            String offlineArchive = createArchive(offlineProfile, txId[0]);

            System.out.println(onlineArchive + " --- " + offlineArchive);

            // Upload data
            String finalTxId = txId[0];

            // Upload offline data
            OperationHelper.doWithRetry(3, new Operation() {
                @Override
                public void run() throws IOException, ImportException {
                    uploadOfflineData(file, offlineArchive, finalTxId);
                }
            });

            // Upload online data
            OperationHelper.doWithRetry(3, new Operation() {
                @Override
                public void run() throws IOException, ImportException {
                    uploadOnlineData(file, onlineArchive, finalTxId);
                }
            });

            //String offlineUrl = uploadOfflineData(file, offlineArchive, txId);
            //String onlineUrl = uploadOnlineData(file, onlineArchive, txId);

            // Commit the transaction
            commitTransaction(txId[0]);

            importResult = new ImportResult(onlineArchive, offlineArchive);

        } catch (Exception e) {
            if (txId[0] != null) {
                rollbackTransaction(txId[0]);
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

    private String uploadOnlineData(File file, String archive, String txId) throws IOException, ImportException {
        System.out.println("Starting to upload online data...");

        // Exclude tif files (*.tiff, *.tif)
        String fullUrl = url + vault + "/" + archive + "?exclude=" + offlineFileTypes;

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Content-Type", "application/zip")
                .addHeader("X-Transaction", txId)
                .post(RequestBody.create(MEDIA_TYPE_ZIP, file))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("Online data was uploaded");
                return response.header("Location");
            }

            System.out.println("Something is wrong. Cannot upload online data");

            // Something is wrong, throw the exception
            ImportException exception = new ImportException("Cannot upload online data");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
        }
    }

    private String uploadOfflineData(File file, String archive, String txId) throws IOException, ImportException {
        System.out.println("Starting to upload offline data...");

        String fullUrl = url + vault + "/" + archive;

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
                System.out.println("Offline data was uploaded");
                return response.header("Location");
            }
            System.out.println("Something is wrong. Cannot upload offline data");

            // Something is wrong, throw the exception
            ImportException exception = new ImportException("Cannot upload offline data");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
        }
    }

    private String createArchive(String profile, String txId) throws IOException, ImportException {
        String fullUrl = url + vault;

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
                if (response.body() != null) {
                    String bodyString = response.body().string();

                    // Parse the returned JSON
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(bodyString).getAsJsonObject();

                    return jsonObject.getAsJsonPrimitive("id").getAsString();
                }
            }

            // Cannot get the transaction ID? Abort!
            ImportException exception = new ImportException("Cannot create an archive");
            exception.setHttpStatusCode(response.code());
            exception.setHttpMessage(response.message());

            throw exception;
        }
    }

    private void commitTransaction(String txId) throws IOException, ImportException {
        System.out.println("Committing the transaction: " + txId);

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
        System.out.println("Transaction committed");
    }

    private void rollbackTransaction(String txId) throws IOException, ImportException {
        System.out.println("Rolling back the transaction: " + txId);

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
        System.out.println("Roll back successfully");
    }
}
