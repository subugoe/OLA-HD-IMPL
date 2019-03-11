package ola.hd.longtermstorage.service;

import ola.hd.longtermstorage.exception.ImportException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;

public interface ImportService {

    /**
     * Import a new ZIP file to the system
     *
     * @param extractedDir The path to the folder where the ZIP file was extracted
     * @param pid          The PID which was assigned for this file
     * @param metaData     The list of meta-data of this ZIP
     * @throws IOException     Thrown if something's wrong when connecting to different services
     * @throws ImportException Thrown if the HTTP Statuses returned from other services are not success
     */
    void importZipFile(Path extractedDir, String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> metaData) throws IOException, ImportException;

    /**
     * Import a new version of a work
     *
     * @param extractedDir The path to the folder where the ZIP file was extracted
     * @param pid          The PID which was assigned for this file
     * @param metaData     The list of meta-data of this ZIP
     * @param prevPid      The PID of the previous version
     * @throws IOException     Thrown if something's wrong when connecting to different services
     * @throws ImportException Thrown if the HTTP Statuses returned from other services are not success
     */
    void importZipFile(Path extractedDir, String pid, List<AbstractMap.SimpleImmutableEntry<String, String>> metaData, String prevPid) throws IOException, ImportException;
}
