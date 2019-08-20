package ola.hd.longtermstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class LongTermStorageApplicationTests {

    @Test
    public void contextLoads() {
    }

//    @Test
    public void deleteAllArchives() throws IOException {

        // Get list of archives
        String url = "https://cdstar.gwdg.de/dev/v3/tdoan/?q=scope:archive&limit=128";
        String username = "tdoan";
        String password = "tdoanpwd";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(username, password))
                .get()
                .build();

        String jsonString = null;
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                jsonString = response.body().string();
            }
        }

        if (jsonString != null) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);
            JsonNode hits = root.get("hits");
            if (hits.isArray()) {

                // Loop through the hits
                for (JsonNode hit : hits) {

                    // Get archive id
                    String id = hit.get("id").asText();

                    // Delete the archive
                    String deleteUrl = "https://cdstar.gwdg.de/dev/v3/tdoan/" + id;

                    request = new Request.Builder()
                            .url(deleteUrl)
                            .addHeader("Authorization", Credentials.basic(username, password))
                            .delete()
                            .build();
                    client.newCall(request).execute();
                }
            }
        }
    }
}
