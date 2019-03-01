package ola.hd.longtermstorage.service;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;

public interface ImportService {
    void importZipFile(Path extractedDir, String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> metaData) throws Exception;
}
