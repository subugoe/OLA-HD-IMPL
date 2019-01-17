package ola.hd.longtermstorage.domain;

import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {

    private List<Pair<String, String>> content;

    public ImportResult() {
        content = new ArrayList<>();
    }

    public void add(String key, String value) {
        Pair<String, String> pair = Pair.of(key, value);
        content.add(pair);
    }

    public List<Pair<String, String>> getContent() {
        return content;
    }
}
