package ola.hd.longtermstorage.service;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ola.hd.longtermstorage.domain.SearchRequest;
import ola.hd.longtermstorage.domain.SearchResult;
import ola.hd.longtermstorage.utils.PicaSaxHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
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
    public List<SearchResult> search(SearchRequest searchRequest) throws IOException, ParserConfigurationException, SAXException {

        // Construct the URL
        HttpUrl httpUrl = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("version", version)
                .addQueryParameter("operation", operation)
                .addQueryParameter("maximumRecords", searchRequest.getLimit() + "")
                .addQueryParameter("query", searchRequest.getQuery())
                .addQueryParameter("recordSchema", "picaxml")
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
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser saxParser = factory.newSAXParser();

                    PicaSaxHandler picaSaxHandler = new PicaSaxHandler();
                    saxParser.parse(response.body().byteStream(), picaSaxHandler);

                    return picaSaxHandler.getExtractedData();
                }
            }

            // Something's wrong with the search? Throw the exception
            throw new HttpServerErrorException(HttpStatus.valueOf(response.code()), "Error when searching with the query " + searchRequest.getQuery());
        }
    }
}
