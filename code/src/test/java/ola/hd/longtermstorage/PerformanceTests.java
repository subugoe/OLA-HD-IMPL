package ola.hd.longtermstorage;

import okhttp3.*;
import ola.hd.longtermstorage.service.PidService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Ignore("Ignore the performance test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class PerformanceTests {

    private final String URL = "http://141.5.105.253:8080/bag";
    private final String TOKEN = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZG9hbiIsImlhdCI6MTU1NzkyMzQyNSwiZXhwIjoxNTU5NzIzNDI1LCJhdXRoIjoiUk9MRV9VU0VSIn0.hq0eJwjpI7Fja4agQhTKINhAJsPDlSf4kbX65JjTLf6-uIOunxqHFoP6nPyfIhIcQ2Vw5QKHmWlbaCLiUy4yVQ";
    private final String APPLICATION_ZIP = "application/zip";
    private final String FILE_PATH = "/Users/tdoan/OCR-D-LZA/sample_data/benner_herrnhuterey04_1748.ocrd.zip";
    private List<String> pids = new ArrayList<>();
    private List<Long> durations = new ArrayList<>();
    private final int TOTAL_UPLOAD = 1000;

    @Autowired
    private PidService pidService;

    @Test
    public void run() throws IOException {
        long start = System.currentTimeMillis();
        IntStream.range(0, TOTAL_UPLOAD)
                .parallel()
                .forEach(i -> {
                    try {
                        upload();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        long end = System.currentTimeMillis();
        long elapsedTime = end - start;
        System.out.println("Elapsed Time: " + elapsedTime);
        writeToFile();
    }

    @After
    public void tearDown() throws IOException {
        for (String pid : pids) {
            pidService.deletePid(pid);
        }
    }

    private void upload() throws IOException {
        OkHttpClient client = new OkHttpClient();

        File file = new File(FILE_PATH);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "benner_herrnhuterey04_1748.ocrd.zip",
                        RequestBody.create(MediaType.parse(APPLICATION_ZIP), file))
                .build();

        Request request = new Request.Builder()
                .url(URL)
                .addHeader("Authorization", TOKEN)
                .addHeader("Content-Type", APPLICATION_ZIP)
                .post(requestBody)
                .build();

        long start = System.currentTimeMillis();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {

                long end = System.currentTimeMillis();
                durations.add(end - start);

                String pid = response.header("Location");
                pids.add(pid);
            }
        }
    }

    private void writeToFile() throws IOException {
        try (PrintStream fileStream = new PrintStream(new File("ola-hd-performance.csv"))) {
            fileStream.println("Duration");

            for (long time : durations) {
                fileStream.println(time);
            }
        }
    }
}
