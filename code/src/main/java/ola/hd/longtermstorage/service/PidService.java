package ola.hd.longtermstorage.service;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;

public interface PidService {
    String createPid(List<AbstractMap.SimpleImmutableEntry<String, String>> data) throws IOException;
    void updatePid(String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> data) throws IOException;
    void appendData(String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> data) throws IOException;
    void deletePid(String pid) throws IOException;
}
