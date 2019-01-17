package ola.hd.longtermstorage.service;

import org.springframework.data.util.Pair;

import java.io.IOException;
import java.util.List;

public interface PidService {
    String createPid(List<Pair<String, String>> data) throws IOException;
    void deletePid(String pid) throws IOException;
}
