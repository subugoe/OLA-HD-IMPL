package ola.hd.longtermstorage.service;

import ola.hd.longtermstorage.domain.ImportResult;
import ola.hd.longtermstorage.exception.ImportException;

import java.io.File;
import java.io.IOException;

public interface ImportService {
    ImportResult importZipFile(File file) throws Exception;
}
