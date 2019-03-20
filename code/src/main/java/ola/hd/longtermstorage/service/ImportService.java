package ola.hd.longtermstorage.service;

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
     * @return Meta-data from the import process (e.g. URL to archive on disk / tape)
     * @throws IOException Thrown if something's wrong when connecting to different services
     */
    List<AbstractMap.SimpleImmutableEntry<String, String>> importZipFile(Path extractedDir,
                                                                         String pid,
                                                                         List<AbstractMap.SimpleImmutableEntry<String, String>> metaData) throws IOException;

    /**
     * Import a new version of a work
     *
     * @param extractedDir The path to the folder where the ZIP file was extracted
     * @param pid          The PID which was assigned for this file
     * @param metaData     The list of meta-data of this ZIP
     * @param prevPid      The PID of the previous version
     * @return Meta-data from the import process (e.g. URL to archive on disk / tape)
     * @throws IOException Thrown if something's wrong when connecting to different services
     */
    List<AbstractMap.SimpleImmutableEntry<String, String>> importZipFile(Path extractedDir,
                                                                         String pid,
                                                                         List<AbstractMap.SimpleImmutableEntry<String, String>> metaData,
                                                                         String prevPid) throws IOException;
}
