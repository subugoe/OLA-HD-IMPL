package ola.hd.longtermstorage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.AbstractMap;
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
    public String createPid(List<AbstractMap.SimpleImmutableEntry<String, String>> data) throws IOException {

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
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(bodyString);
                    return root.get("epic-pid").asText();
                }
            }
        }

        return null;
    }

    @Override
    public boolean updatePid(String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> data) throws IOException {
        String fullUrl = url + pid;
        String payload = buildRequestPayload(data);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .put(RequestBody.create(MEDIA_TYPE_JSON, payload))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return true;
            }
        }

        return false;
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

    private String buildRequestPayload(List<AbstractMap.SimpleImmutableEntry<String, String>> data) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (AbstractMap.SimpleImmutableEntry<String, String> pair: data) {
            ObjectNode node = mapper.createObjectNode();
            node.put("type", pair.getKey());
            node.put("parsed_data", pair.getValue());

            arrayNode.add(node);
        }

        return mapper.writeValueAsString(arrayNode);
    }
}
