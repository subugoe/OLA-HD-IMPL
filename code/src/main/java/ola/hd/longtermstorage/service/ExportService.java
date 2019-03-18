package ola.hd.longtermstorage.service;

import java.io.IOException;

public interface ExportService {
    byte[] export(String id) throws IOException;
}
