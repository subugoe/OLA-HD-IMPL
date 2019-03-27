package ola.hd.longtermstorage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import ola.hd.longtermstorage.component.MutexFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
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

    private final MutexFactory<String> mutexFactory;

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

    @Autowired
    public EpicPidService(MutexFactory<String> mutexFactory) {
        this.mutexFactory = mutexFactory;
    }

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

            // Something is wrong. Throw exception
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot create a PID.");
        }
    }

    @Override
    public void updatePid(String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> data) throws IOException {
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
            if (!response.isSuccessful()) {

                // Something is wrong. Throw exception
                throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot update the PID " + pid);
            }
        }
    }

    @Override
    public void appendData(String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> data) throws IOException {

        // Execute sequentially if it tries to append to the same PID
        synchronized (mutexFactory.getMutex(pid)) {

            // Get current data of the PID
            List<AbstractMap.SimpleImmutableEntry<String, String>> pidData = getPidData(pid);

            // Append new data
            pidData.addAll(data);

            // Update the PID
            updatePid(pid, pidData);
        }
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

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {

                // Something is wrong. Throw exception
                throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot delete the PID " + pid);
            }
        }
    }

    private List<AbstractMap.SimpleImmutableEntry<String, String>> getPidData(String pid) throws IOException {
        List<AbstractMap.SimpleImmutableEntry<String, String>> data = new ArrayList<>();

        String fullUrl = url + pid;
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", Credentials.basic(username, password))
                .addHeader("Accept", "application/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    String bodyString = response.body().string();

                    // Parse the returned JSON
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(bodyString);

                    for (JsonNode item : root) {
                        String type = item.get("type").asText();

                        // Ignore HS_ADMIN
                        if (!type.equals("HS_ADMIN")) {
                            JsonNode valueNode = item.get("parsed_data");

                            // Only take if it's a string
                            if (valueNode.isTextual()) {
                                String value = valueNode.asText();
                                data.add(new AbstractMap.SimpleImmutableEntry<>(type, value));
                            }
                        }
                    }
                }
            } else {
                // Something is wrong. Throw exception
                throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Cannot get data of the PID " + pid);
            }
        }

        return data;
    }

    private String buildRequestPayload(List<AbstractMap.SimpleImmutableEntry<String, String>> data) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (AbstractMap.SimpleImmutableEntry<String, String> pair: data) {
            ObjectNode node = mapper.createObjectNode();
            node.put("type", pair.getKey().toUpperCase());
            node.put("parsed_data", pair.getValue());

            arrayNode.add(node);
        }

        return mapper.writeValueAsString(arrayNode);
    }
}
