package ola.hd.longtermstorage.controller;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.*;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import ola.hd.longtermstorage.domain.Action;
import ola.hd.longtermstorage.domain.ResponseMessage;
import ola.hd.longtermstorage.domain.Status;
import ola.hd.longtermstorage.domain.TrackingInfo;
import ola.hd.longtermstorage.repository.TrackingRepository;
import ola.hd.longtermstorage.service.ImportService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private ImportService importService;

    private TrackingRepository trackingRepository;

    // TODO: Use ExecutorService to parallelized the code

    @Autowired
    public ImportController(ImportService importService, TrackingRepository trackingRepository) {
        this.importService = importService;
        this.trackingRepository = trackingRepository;
    }

    @PostMapping(value = "/bag", produces = "application/json")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file) {

        String originalName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalName);

        // A file was uploaded
        TrackingInfo info = new TrackingInfo("user", Action.CREATE, originalName, new Date(), Status.PROCESSING);
        trackingRepository.save(info);

        // Not a zip file
        if (extension == null || !extension.equals("zip")) {
            info.setStatus(Status.FAILED);
            info.setMessage("The input must be a zip file");
            trackingRepository.save(info);

            return new ResponseEntity<>(
                    new ResponseMessage(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The input must be a zip file"),
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        // For a unique directory name for each uploaded file
        UUID uuid = UUID.randomUUID();

        // Save the uploaded file to the temp folder
        File targetFile = new File("tmp" + File.separator + uuid + File.separator + originalName);

        try (InputStream fileStream = file.getInputStream()) {

            // Use file stream to prevent memory overflowed due to the huge file size
            FileUtils.copyInputStreamToFile(fileStream, targetFile);

            // Extract zip file
            ZipFile zipFile = new ZipFile(targetFile);
            String destination = targetFile.getParentFile().getPath();
            zipFile.extractAll(destination);

        } catch (IOException | ZipException e) {
            e.printStackTrace();
            logger.info(e.getMessage(), e);

            info.setStatus(Status.FAILED);
            info.setMessage(e.getMessage());
            trackingRepository.save(info);

            return new ResponseEntity<>(
                    new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Create the path to the extracted directory
        String pathToExtractedDir = targetFile.getParent() + File.separator + FilenameUtils.getBaseName(targetFile.getName());
        Path rootDir = Paths.get(pathToExtractedDir);

        BagReader reader = new BagReader();
        try {
            // Create a bag from an existing directory
            Bag bag = reader.read(rootDir);

            BagVerifier verifier = new BagVerifier();

            // Check for the validity and completeness of a bag
            verifier.isValid(bag, true);

            if (BagVerifier.canQuickVerify(bag)) {
                BagVerifier.quicklyVerify(bag);
            }

            // TODO: Upload the zip file to CDSTAR
            //importService.importZipFile(targetFile);

        } catch (MissingPayloadManifestException | UnsupportedAlgorithmException | MaliciousPathException |
                InvalidPayloadOxumException | MissingPayloadDirectoryException | FileNotInPayloadDirectoryException |
                UnparsableVersionException | InvalidBagitFileFormatException | MissingBagitFileException |
                CorruptChecksumException | VerificationException e) {
            e.printStackTrace();
            logger.info(e.getMessage(), e);

            info.setStatus(Status.FAILED);
            info.setMessage(e.getMessage());
            trackingRepository.save(info);

            return new ResponseEntity<>(
                    new ResponseMessage(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            logger.info(e.getMessage(), e);

            info.setStatus(Status.FAILED);
            info.setMessage(e.getMessage());
            trackingRepository.save(info);

            return new ResponseEntity<>(
                    new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Save success information
        info.setStatus(Status.SUCCESS);
        info.setMessage("Your file was successfully uploaded");
        trackingRepository.save(info);

        return new ResponseEntity<>(
                new ResponseMessage(HttpStatus.OK, "Your file was successfully uploaded"),
                HttpStatus.OK);
    }
}
