package ola.hd.longtermstorage.service;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(CdstarService.class);
    private static final MediaType MEDIA_TYPE_ZIP = MediaType.parse("application/zip");

    public void importZipFile(File file) throws IOException {

        // TODO: Extract meta-data

        // TODO: Use transaction to upload files
        uploadOnlineData(file);
        uploadOfflineData(file);

        // TODO: Assign PID
    }

    private void uploadOnlineData(File file) throws IOException {

        // TODO: exclude tif files (*.tiff, *.tif)
        String fullUrl = url + vault;

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Content-Type", "application/zip")
                .post(RequestBody.create(MEDIA_TYPE_ZIP, file))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {

                // TODO: update the management database
                logger.info(response.header("Location"));
            }
        }

        // TODO: update the location of the offline data? Or link them through PID
    }

    private void uploadOfflineData(File file) throws IOException {
        String fullUrl = url + vault;

        OkHttpClient client = new OkHttpClient();

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
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {

                // TODO: update the management database, get the file location
                logger.info(response.header("Location"));
            } else {
                // TODO: update the management database
                logger.info(response.message());
            }
        }
    }
}
