package ola.hd.longtermstorage.controller;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.*;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import io.swagger.annotations.*;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import ola.hd.longtermstorage.component.ExecutorWrapper;
import ola.hd.longtermstorage.component.MutexFactory;
import ola.hd.longtermstorage.domain.*;
import ola.hd.longtermstorage.repository.mongo.ArchiveRepository;
import ola.hd.longtermstorage.repository.mongo.TrackingRepository;
import ola.hd.longtermstorage.service.ArchiveManagerService;
import ola.hd.longtermstorage.service.PidService;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.ServletWebRequest;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Api(description = "This endpoint is used to import a ZIP file into the system")
@RestController
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    private final ArchiveManagerService archiveManagerService;

    private final TrackingRepository trackingRepository;

    private final ArchiveRepository archiveRepository;

    private final PidService pidService;

    private final ExecutorWrapper executor;

    private final MutexFactory<String> mutexFactory;

    @Value("${ola.hd.upload.dir}")
    private String uploadDir;

    @Autowired
    public ImportController(ArchiveManagerService archiveManagerService, TrackingRepository trackingRepository, ArchiveRepository archiveRepository, PidService pidService,
                            ExecutorWrapper executor, MutexFactory<String> mutexFactory) {
        this.archiveManagerService = archiveManagerService;
        this.trackingRepository = trackingRepository;
        this.archiveRepository = archiveRepository;
        this.pidService = pidService;
        this.executor = executor;
        this.mutexFactory = mutexFactory;
    }

    @ApiOperation(value = "Import a ZIP file into a system. It may be an independent ZIP, or a new version of another ZIP. " +
            "In the second case, a PID of the previous ZIP must be provided.",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            authorizations = {
                    @Authorization(value = "basicAuth"),
                    @Authorization(value = "bearer")
            })
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The ZIP has a valid BagIt structure. The system is saving it to the archive.",
                    response = ResponseMessage.class,
                    responseHeaders = {
                            @ResponseHeader(name = "Location", description = "The PID of the ZIP.", response = String.class)
                    }),
            @ApiResponse(code = 400, message = "The ZIP has an invalid BagIt structure.", response = ResponseMessage.class),
            @ApiResponse(code = 401, message = "Invalid credentials.", response = ResponseMessage.class),
            @ApiResponse(code = 415, message = "The request is not a multipart request.", response = ResponseMessage.class)
    })
    @ApiImplicitParams(value = {
            @ApiImplicitParam(dataType = "__file", name = "file", value = "The file to be imported.", required = true, paramType = "form"),
            @ApiImplicitParam(dataType = "string", name = "prev", value = "The PID of the previous version", paramType = "form")
    })
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/bag",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> importData(HttpServletRequest request, @ApiIgnore Principal principal) throws IOException, FileUploadException {

        String username = principal.getName();
        TrackingInfo info = new TrackingInfo(username, TrackingStatus.PROCESSING, "Processing...", null);

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {

            String message = "The request must be multipart request.";

            // Save to the tracking database
            info.setStatus(TrackingStatus.FAILED);
            info.setMessage(message);
            trackingRepository.save(info);

            throw new HttpClientErrorException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
        }

        // A PID pointing to the previous version
        String prev = null;

        // For a unique directory name for each uploaded file
        UUID uuid = UUID.randomUUID();

        // Temporary directory
        String tempDir = uploadDir + File.separator + uuid;

        // The uploaded file
        File targetFile = null;

        // Make sure that there is only 1 file uploaded
        int fileCount = 0;

        // Read the upload stream
        ServletFileUpload upload = new ServletFileUpload();
        FileItemIterator iterStream = upload.getItemIterator(request);
        while (iterStream.hasNext()) {

            FileItemStream item = iterStream.next();

            // Is it a file?
            if (!item.isFormField()) {

                // Get file name
                String fileName = item.getName();

                // Save the uploaded file to the temp folder
                targetFile = new File(tempDir + File.separator + fileName);

                fileCount++;

                // More than 1 file is uploaded?
                if (fileCount > 1) {

                    // Clean up the temp
                    FileSystemUtils.deleteRecursively(new File(tempDir));

                    String message = "Only 1 zip file is allow.";

                    // Save to the tracking database
                    info.setStatus(TrackingStatus.FAILED);
                    info.setMessage(message);
                    trackingRepository.save(info);

                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
                }

                // Save the file
                try (InputStream uploadedStream = item.openStream();
                     OutputStream out = FileUtils.openOutputStream(targetFile)) {
                    IOUtils.copy(uploadedStream, out);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    throw new HttpServerErrorException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "The upload process was interrupted. Please try again.");
                }

            } else {

                try (InputStream stream = item.openStream()) {
                    String formFieldName = item.getFieldName();

                    // New version of a bag
                    if (formFieldName.equals("prev")) {
                        prev = Streams.asString(stream);
                    }
                }
            }
        }

        // No file is uploaded?
        if (targetFile == null) {

            String message = "The request must contain 1 zip file.";

            // Save to the tracking database
            info.setStatus(TrackingStatus.FAILED);
            info.setMessage(message);
            trackingRepository.save(info);

            // Throw a friendly message to the client
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
        }

        // Not a ZIP file?
        Tika tika = new Tika();
        String mimeType = tika.detect(targetFile);
        if (!mimeType.equals("application/zip")) {

            // Clean up the temp
            FileSystemUtils.deleteRecursively(new File(tempDir));

            String message = "The file must be in the ZIP format";

            // Save to the tracking database
            info.setStatus(TrackingStatus.FAILED);
            info.setMessage(message);
            trackingRepository.save(info);

            // Throw a friendly message to the client
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
        }

        // Where to extract the file
        // Add the _extracted to avoid naming conflict with the targetFile.
        String destination = tempDir + File.separator + FilenameUtils.getBaseName(targetFile.getName()) + "_extracted";

        Bag bag;
        try {
            // Extract the zip file
            ZipFile zipFile = new ZipFile(targetFile);
            zipFile.extractAll(destination);

            // Validate the bag
            Path rootDir = Paths.get(destination);
            BagReader reader = new BagReader();

            // Create a bag from an existing directory
            bag = reader.read(rootDir);

            BagVerifier verifier = new BagVerifier();

            if (BagVerifier.canQuickVerify(bag)) {
                BagVerifier.quicklyVerify(bag);
            }

            // Check for the validity and completeness of a bag
            verifier.isValid(bag, true);

        } catch (NoSuchFileException | MissingPayloadManifestException | UnsupportedAlgorithmException | CorruptChecksumException |
                MaliciousPathException | InvalidPayloadOxumException | FileNotInPayloadDirectoryException |
                MissingPayloadDirectoryException | InvalidBagitFileFormatException | InterruptedException |
                ZipException | UnparsableVersionException | MissingBagitFileException | VerificationException ex) {

            // Clean up the temp
            FileSystemUtils.deleteRecursively(new File(tempDir));

            String message = "Invalid file input. The uploaded file must be a ZIP file with BagIt structure.";

            // Save to the tracking database
            info.setStatus(TrackingStatus.FAILED);
            info.setMessage(message);
            trackingRepository.save(info);

            // Throw a friendly message to the client
            throw new IllegalArgumentException(message, ex);
        }

        // Get meta-data from bag-info.txt
        List<AbstractMap.SimpleImmutableEntry<String, String>> bagInfos = bag.getMetadata().getAll();

        // Retry policies when a call to another service is failed
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .withDelay(Duration.ofSeconds(10))
                .withMaxRetries(3);

        // Create a PID with meta-data from bag-info.txt
        String pid = Failsafe.with(retryPolicy).get(() -> pidService.createPid(bagInfos));

        // Store the PID to the tracking database
        info.setPid(pid);

        // Build the export URL
        WebMvcLinkBuilder linkBuilder = WebMvcLinkBuilder.linkTo(
                WebMvcLinkBuilder.methodOn(ExportController.class).export(pid, false));
        String exportUrl = linkBuilder.toString();

        if (prev != null) {

            // Import a new version of a bag
            String finalPrev = prev;
            executor.submit(() -> {
                ImportResult importResult = null;
                try {
                    // Import files
                    importResult = Failsafe.with(retryPolicy).get(
                            () -> archiveManagerService.importZipFile(
                                    Paths.get(destination), pid, bagInfos, finalPrev));

                    List<AbstractMap.SimpleImmutableEntry<String, String>> metaData = importResult.getMetaData();

                    // Point to the previous version
                    metaData.add(new AbstractMap.SimpleImmutableEntry<>("PREVIOUS-VERSION", finalPrev));

                    // Meta-data from the bag-info.txt
                    metaData.addAll(bagInfos);

                    // The export URL
                    metaData.add(new AbstractMap.SimpleImmutableEntry<>("URL", exportUrl));

                    // Use update instead of append to save 1 HTTP call to the PID Service
                    pidService.updatePid(pid, metaData);

                    // Update the old PID to link to the new version
                    List<AbstractMap.SimpleImmutableEntry<String, String>> pidAppendedData = new ArrayList<>();
                    pidAppendedData.add(new AbstractMap.SimpleImmutableEntry<>("NEXT-VERSION", pid));
                    pidService.appendData(finalPrev, pidAppendedData);


                    // Save success data to the tracking database
                    info.setStatus(TrackingStatus.SUCCESS);
                    info.setMessage("Data has been successfully imported.");
                    trackingRepository.save(info);

                    // Create new archive in the database
                    Archive archive = new Archive(pid, importResult.getOnlineId(), importResult.getOfflineId());

                    // Execute sequentially if it tries to change the same document
                    synchronized (mutexFactory.getMutex(finalPrev)) {

                        // Find the previous version
                        Archive prevVersion = archiveRepository.findByPid(finalPrev);

                        // Link to previous version
                        archive.setPreviousVersion(prevVersion);

                        // Remove online-id of the previous version
                        prevVersion.setOnlineId(null);

                        // Set Next Version field
                        prevVersion.addNextVersion(archive);

                        archiveRepository.save(archive);
                        archiveRepository.save(prevVersion);
                    }

                } catch (Exception ex) {
                    handleFailedImport(ex, pid, importResult, info);
                } finally {
                    // Clean up the temp
                    FileSystemUtils.deleteRecursively(new File(tempDir));
                }
            });
        } else {

            // Import an individual bag
            executor.submit(() -> {
                ImportResult importResult = null;
                try {
                    // Import files
                    importResult = Failsafe.with(retryPolicy).get(
                            () -> archiveManagerService.importZipFile(Paths.get(destination), pid, bagInfos));

                    List<AbstractMap.SimpleImmutableEntry<String, String>> metaData = importResult.getMetaData();

                    // Meta-data from the bag-info.txt
                    metaData.addAll(bagInfos);

                    // The export URL
                    metaData.add(new AbstractMap.SimpleImmutableEntry<>("URL", exportUrl));

                    // Use update instead of append to save 1 HTTP call to the PID Service
                    pidService.updatePid(pid, metaData);

                    // Save success data to the tracking database
                    info.setStatus(TrackingStatus.SUCCESS);
                    info.setMessage("Data has been successfully imported.");
                    trackingRepository.save(info);

                    // Create new archive in the database
                    Archive archive = new Archive(pid, importResult.getOnlineId(), importResult.getOfflineId());
                    archiveRepository.save(archive);

                } catch (Exception ex) {
                    handleFailedImport(ex, pid, importResult, info);
                } finally {
                    // Clean up the temp
                    FileSystemUtils.deleteRecursively(new File(tempDir));
                }
            });
        }

        trackingRepository.save(info);

        ResponseMessage responseMessage = new ResponseMessage(HttpStatus.ACCEPTED, "Your data is being processed.");
        responseMessage.setPid(pid);

        return ResponseEntity.accepted().body(responseMessage);
    }

    private void handleFailedImport(Exception ex, String pid, ImportResult importResult, TrackingInfo info) {

        try {
            // Delete the PID
            pidService.deletePid(pid);

            // Delete the archive
            if (importResult != null) {
                archiveManagerService.deleteArchive(importResult.getOnlineId(), null);
                archiveManagerService.deleteArchive(importResult.getOfflineId(), null);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Log the error
        logger.error(ex.getMessage(), ex);

        // Save the failure data to the tracking database
        info.setStatus(TrackingStatus.FAILED);
        info.setMessage(ex.getMessage());

        // Delete the PID in the tracking database
        info.setPid(null);

        trackingRepository.save(info);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, ServletWebRequest request) {

        // Extract necessary information
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMessage();
        String uri = request.getRequest().getRequestURI();

        // Log the error
        logger.error(message, ex);

        return ResponseEntity.badRequest()
                .body(new ResponseMessage(status, message, uri));
    }
}
