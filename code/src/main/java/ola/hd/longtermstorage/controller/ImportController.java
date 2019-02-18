package ola.hd.longtermstorage.controller;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.*;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import ola.hd.longtermstorage.domain.*;
import ola.hd.longtermstorage.repository.TrackingRepository;
import ola.hd.longtermstorage.service.ImportService;
import ola.hd.longtermstorage.service.PidService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

@RestController
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    private final ImportService importService;

    private final PidService pidService;

    private final TrackingRepository trackingRepository;

    // TODO: Use ExecutorService to parallelize the code

    @Autowired
    public ImportController(ImportService importService, PidService pidService, TrackingRepository trackingRepository) {
        this.importService = importService;
        this.pidService = pidService;
        this.trackingRepository = trackingRepository;
    }

    @PostMapping(value = "/bag", produces = "application/json")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file) {

        String originalName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalName);

        // A file was uploaded
        TrackingInfo info = new TrackingInfo("user",
                Action.CREATE,
                originalName,
                new Date(),
                Status.PROCESSING);
        trackingRepository.save(info);

        // Not a zip file
        if (extension == null || !extension.equals("zip")) {
            info.setStatus(Status.FAILED);
            info.setMessage("The input must be a zip file");
        } else {

            // For a unique directory name for each uploaded file
            UUID uuid = UUID.randomUUID();

            // Save the uploaded file to the temp folder
            File targetFile = new File("upload-temp" + File.separator + uuid + File.separator + originalName);

            // Is the file successfully extracted?
            boolean isExtracted = false;

            try (InputStream fileStream = file.getInputStream()) {

                // Use file stream to prevent memory overflowed due to the huge file size
                FileUtils.copyInputStreamToFile(fileStream, targetFile);

                // Extract zip file
                ZipFile zipFile = new ZipFile(targetFile);
                String destination = targetFile.getParentFile().getPath();
                zipFile.extractAll(destination);

                // Successfully extracted the file
                isExtracted = true;

            } catch (IOException | ZipException e) {
                logger.error(e.getMessage(), e);

                info.setStatus(Status.FAILED);
                info.setMessage(e.getMessage());
            }

            if (isExtracted) {

                // Create the path to the extracted directory
                String pathToExtractedDir = targetFile.getParent() + File.separator + FilenameUtils.getBaseName(targetFile.getName());
                Path rootDir = Paths.get(pathToExtractedDir);

                BagReader reader = new BagReader();

                boolean isValidBag = false;
                try {
                    // Create a bag from an existing directory
                    Bag bag = reader.read(rootDir);

                    BagVerifier verifier = new BagVerifier();

                    if (BagVerifier.canQuickVerify(bag)) {
                        BagVerifier.quicklyVerify(bag);
                    }

                    // Check for the validity and completeness of a bag
                    verifier.isValid(bag, true);

                    isValidBag = true;

                } catch (MissingPayloadManifestException | UnsupportedAlgorithmException |
                        MaliciousPathException | InvalidPayloadOxumException | MissingPayloadDirectoryException |
                        FileNotInPayloadDirectoryException | UnparsableVersionException |
                        InvalidBagitFileFormatException | MissingBagitFileException | CorruptChecksumException |
                        VerificationException | IOException | InterruptedException e) {
                    logger.error(e.getMessage(), e);

                    info.setStatus(Status.FAILED);
                    info.setMessage(e.getMessage());
                }

                // Only import valid bag
                if (isValidBag) {

                    try {

                        // Import data to the archive storage
                        String pid = importService.importZipFile(targetFile, rootDir);

                        info.setStatus(Status.SUCCESS);
                        info.setMessage("The file is successfully stored in the system");
                        info.setPid(pid);

                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);

                        info.setStatus(Status.FAILED);
                        info.setMessage(e.getMessage());
                    }

                    // Successfully imported?
//                    if (importResult != null) {
//
//                        try {
//
//                            // Generate a PID
//                            String pid = pidService.createPid(importResult.getContent());
//
//                            info.setStatus(Status.SUCCESS);
//                            info.setMessage("The file is successfully stored in the system");
//                            info.setPid(pid);
//
//                            System.out.println("PID: " + pid);
//
//                        } catch (IOException e) {
//                            logger.error(e.getMessage(), e);
//
//                            info.setStatus(Status.FAILED);
//                            info.setMessage(e.getMessage());
//                        }
//                    }
                }
            }

            // Clean up the temp
            FileSystemUtils.deleteRecursively(targetFile.getParentFile());
        }

        trackingRepository.save(info);

        return new ResponseEntity<>(
                new ResponseMessage(HttpStatus.OK, "Your file was successfully uploaded"),
                HttpStatus.OK);
    }
}
