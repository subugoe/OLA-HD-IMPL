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

    @Value("${cdstar.mainVault}")
    private String mainVault;

    @Value("${cdstar.archiveVault}")
    private String archiveVault;

    private static final Logger logger = LoggerFactory.getLogger(CdstarService.class);
    private static final MediaType MEDIA_TYPE_ZIP = MediaType.parse("application/zip");

    public void importZipFile(File file) throws IOException {

        // TODO: Extract meta-data

        sendToMainVault(file);
        sendToArchiveVault(file);
    }

    private void sendToMainVault(File file) throws IOException {
        String fullUrl = url + mainVault;

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Content-Type", "application/zip")
                .post(RequestBody.create(MEDIA_TYPE_ZIP, file))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String result = response.body().string();

                // TODO: update the management database
                logger.info(result);

            }
        }
    }

    private void sendToArchiveVault(File file) throws IOException {
        String fullUrl = url + archiveVault;

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("/" + file.getName(), file.getName(),
                        RequestBody.create(MEDIA_TYPE_ZIP, file))
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Content-Type", "application/zip")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String result = response.body().string();

                // TODO: update the management database
                logger.info(result);

            }
        }
    }
}
