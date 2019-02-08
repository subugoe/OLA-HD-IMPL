package ola.hd.longtermstorage.service;

import ola.hd.longtermstorage.domain.ImportResult;
import ola.hd.longtermstorage.exception.ImportException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface ImportService {
    ImportResult importZipFile(File file, Path extractedDir) throws Exception;
}
