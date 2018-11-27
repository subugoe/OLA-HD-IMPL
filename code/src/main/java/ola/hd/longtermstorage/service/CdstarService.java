package ola.hd.longtermstorage.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

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

    public String importZipFile(File file) throws IOException {
        String fullUrl = url + mainVault + "?exclude=*.tif";
        String result = "";
        Response response = null;

        try {
            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("/" + file.getName(), file.getName(),
                            RequestBody.create(MediaType.parse("application/zip"), file))
                    .build();

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", Credentials.basic(username, password))
                    .post(formBody)
                    .build();
            response = client.newCall(request).execute();

            if (response.body() != null) {
                result = response.body().string();
            }

        } finally {
            if (response != null) {
                response.close();
            }
        }

        return result;
    }
}
