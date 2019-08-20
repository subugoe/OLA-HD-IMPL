package ola.hd.longtermstorage.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;

public interface ArchiveManagerService {

    /**
     * Import a new ZIP file to the system
     *
     * @param extractedDir The path to the folder where the ZIP file was extracted
     * @param pid          The PID which was assigned for this file
     * @param metaData     The list of meta-data of this ZIP
     * @return             Meta-data from the import process (e.g. URL to archive on disk / tape)
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
     * @return             Meta-data from the import process (e.g. URL to archive on disk / tape)
     * @throws IOException Thrown if something's wrong when connecting to different services
     */
    List<AbstractMap.SimpleImmutableEntry<String, String>> importZipFile(Path extractedDir,
                                                                         String pid,
                                                                         List<AbstractMap.SimpleImmutableEntry<String, String>> metaData,
                                                                         String prevPid) throws IOException;

    /**
     * Export an archive from the hard drive
     *
     * @param identifier   The public identifier of the archive (PID, PPN,...)
     * @param type         Full export or quick export
     * @return             The zip file of the archive
     * @throws IOException Thrown if something's wrong when connecting to the archive system
     */
    byte[] export(String identifier, String type) throws IOException;

    /**
     * Move an archive from a tape to a hard drive
     *
     * @param identifier   The public identifier of the archive (PID, PPN,...)
     * @throws IOException Thrown if something's wrong when connecting to the archive services
     */
    void moveFromTapeToDisk(String identifier) throws IOException;

    /**
     * Move an archive from a hard drive to a tape
     *
     * @param identifier   The public identifier of the archive (PID, PPN,...)
     * @throws IOException Thrown if something's wrong when connecting to the archive services
     */
    void moveFromDiskToTape(String identifier) throws IOException;

    /**
     * Check if an archive is on disk so that it can be exported.
     * @param identifier   The public identifier of the archive (PID, PPN,...)
     * @return             True if the archive is ready on the hard drive, false otherwise.
     * @throws IOException Thrown if something's wrong when connecting to the archive services
     */
    boolean isArchiveOnDisk(String identifier) throws IOException;
}