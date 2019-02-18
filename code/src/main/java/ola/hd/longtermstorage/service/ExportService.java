package ola.hd.longtermstorage.service;

import ola.hd.longtermstorage.exception.ImportException;

import java.io.IOException;

public interface ExportService {
    byte[] export(String id) throws IOException, ImportException;
}
