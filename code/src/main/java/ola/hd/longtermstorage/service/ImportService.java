package ola.hd.longtermstorage.service;

import java.io.File;
import java.nio.file.Path;

public interface ImportService {
    String importZipFile(File file, Path extractedDir) throws Exception;
}
