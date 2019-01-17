package ola.hd.longtermstorage.service;

import com.google.gson.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class EpicPidService implements PidService {

    @Value("${epic.username}")
    private String username;

    @Value("${epic.password}")
    private String password;

    @Value("${epic.prefix}")
    private String prefix;

    @Value("${epic.url}")
    private String url;

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

    @Override
    public String createPid(List<Pair<String, String>> data) throws IOException {

        String fullUrl = url + prefix;
        String payload = buildRequestPayload(data);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(RequestBody.create(MEDIA_TYPE_JSON, payload))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    String bodyString = response.body().string();

                    // Parse the returned JSON
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(bodyString).getAsJsonObject();

                    return jsonObject.getAsJsonPrimitive("epic-pid").getAsString();
                }
            }
        }

        return null;
    }

    @Override
    public void deletePid(String pid) throws IOException {
        String fullUrl = url + pid;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .delete()
                .build();

        client.newCall(request).execute();
    }

    private String buildRequestPayload(List<Pair<String, String>> data) {

        JsonArray jsonArray = new JsonArray();

        for (Pair<String, String> pair: data) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", pair.getFirst());
            jsonObject.addProperty("parsed_data", pair.getSecond());

            jsonArray.add(jsonObject);
        }

        Gson gson = new Gson();
        return gson.toJson(jsonArray);
    }
}
