package ola.hd.longtermstorage.service;

import java.io.File;
import java.io.IOException;

public interface ImportService {
    void importZipFile(File file) throws IOException;
}
