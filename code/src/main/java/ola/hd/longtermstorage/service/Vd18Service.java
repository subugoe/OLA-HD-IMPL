package ola.hd.longtermstorage.service;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ola.hd.longtermstorage.domain.SearchRequest;
import ola.hd.longtermstorage.domain.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class Vd18Service implements SearchService {

    @Value("${search.url}")
    private String url;

    @Value("${search.version}")
    private String version;

    @Value("${search.operation}")
    private String operation;

    @Override
    public List<SearchResult> search(SearchRequest searchRequest) throws IOException {

        String encodedQuery = URLEncoder.encode(searchRequest.getQuery(), StandardCharsets.UTF_8);

        // Construct the URL
        HttpUrl httpUrl = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("version", version)
                .addQueryParameter("operation", operation)
                .addQueryParameter("maximumRecords", searchRequest.getLimit() + "")
                .addQueryParameter("query", encodedQuery)
                .build();

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(httpUrl)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {

                    // TODO: extract the PPN and title of each result
                }
            }

            // Something's wrong with the search? Throw the exception
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Error when searching with the query " + searchRequest.getQuery());
        }
    }
}
