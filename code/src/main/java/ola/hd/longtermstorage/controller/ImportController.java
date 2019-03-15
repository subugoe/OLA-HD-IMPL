package ola.hd.longtermstorage.controller;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.*;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import io.swagger.annotations.*;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import ola.hd.longtermstorage.component.ExecutorWrapper;
import ola.hd.longtermstorage.domain.ResponseMessage;
import ola.hd.longtermstorage.repository.TrackingRepository;
import ola.hd.longtermstorage.service.ImportService;
import ola.hd.longtermstorage.service.PidService;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Api(description = "This endpoint is used to import a ZIP file into the system")
@RestController
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    private final ImportService importService;

    private final TrackingRepository trackingRepository;

    private final PidService pidService;

    private final ExecutorWrapper executor;

    @Autowired
    public ImportController(ImportService importService, TrackingRepository trackingRepository, PidService pidService, ExecutorWrapper executor) {
        this.importService = importService;
        this.trackingRepository = trackingRepository;
        this.pidService = pidService;
        this.executor = executor;
    }

    @ApiOperation(value = "Import a ZIP file into a system. It may be an independent ZIP, or a new version of another ZIP. " +
            "In the second case, a PID of the previous ZIP must be provided.")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The ZIP has a valid BagIt structure. The system is saving it to the archive.",
                    response = ResponseMessage.class,
                    responseHeaders = {
                            @ResponseHeader(name = "Location", description = "The PID of the ZIP.", response = String.class)
                    }),
            @ApiResponse(code = 400, message = "The ZIP has an invalid BagIt structure.", response = ResponseMessage.class),
            @ApiResponse(code = 415, message = "The request is not a multipart request.", response = ResponseMessage.class)
    })
    @ApiImplicitParams(value = {
            @ApiImplicitParam(dataType = "__file", name = "file", value = "The file to be imported.", required = true, paramType = "form"),
            @ApiImplicitParam(dataType = "String", name = "prev", value = "The PID of the previous version", paramType = "form")
    })
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping(value = "/bag", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importData(HttpServletRequest request) throws IOException, FileUploadException, URISyntaxException {

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            throw new HttpClientErrorException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request must be multipart request");
        }

        // A PID pointing to the previous version
        String prev = null;

        // For a unique directory name for each uploaded file
        UUID uuid = UUID.randomUUID();

        // Save the uploaded file to the temp folder
        File targetFile = new File("upload-temp" + File.separator + uuid + File.separator + "temp.zip");

        // Where to extract the file
        String destination = targetFile.getParent() + File.separator + FilenameUtils.getBaseName(targetFile.getName());

        // Make sure that there is only 1 file uploaded
        int fileCount = 0;

        // Read the upload stream
        ServletFileUpload upload = new ServletFileUpload();
        FileItemIterator iterStream = upload.getItemIterator(request);
        while (iterStream.hasNext()) {

            FileItemStream item = iterStream.next();

            // Is it a file?
            if (!item.isFormField()) {

                fileCount++;

                // More than 1 file is uploaded?
                if (fileCount > 1) {

                    // Clean up the temp
                    FileSystemUtils.deleteRecursively(targetFile.getParentFile());

                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Only 1 zip file is allow");
                }

                // Check file type
//                try (InputStream uploadedStream = item.openStream()) {
//
//                    Tika tika = new Tika();
//                    String mimeType = tika.detect(uploadedStream);
//
//                    // Not a zip file
//                    if (!mimeType.equals("application/zip")) {
//                        return ResponseEntity.badRequest()
//                                .body(new ResponseMessage(HttpStatus.BAD_REQUEST,
//                                        "The file must be in zip format"));
//                    }
//                }

                // Save the file
                try (InputStream uploadedStream = item.openStream();
                     OutputStream out = FileUtils.openOutputStream(targetFile)) {
                    IOUtils.copy(uploadedStream, out);
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
            FileSystemUtils.deleteRecursively(targetFile.getParentFile());

            // Throw a friendly message to the client
            String message = "Invalid file input. The uploaded file must be a ZIP file with BagIt structure.";
            throw new IllegalArgumentException(message, ex);
        }


        // Build meta-data for the PID
        List<AbstractMap.SimpleImmutableEntry<String, String>> data = new ArrayList<>();
        data.add(new AbstractMap.SimpleImmutableEntry<>("ONLINE-URL", "This will be updated soon"));
        data.add(new AbstractMap.SimpleImmutableEntry<>("OFFLINE-URL", "This will be updated soon"));

        // Get meta-data from bag-info.txt
        List<AbstractMap.SimpleImmutableEntry<String, String>> bagInfos = bag.getMetadata().getAll();
        data.addAll(bagInfos);

        // Get an empty PID
        String pid = pidService.createPid(data);

        if (prev != null) {
            // Import a new version of a bag
            System.out.println("Importing a new version");
            String finalPrev = prev;
            executor.submit(() -> {
                try {
                    importService.importZipFile(Paths.get(destination), pid, bagInfos, finalPrev);
                } catch (Exception ex) {
                    // Log the error
                    logger.error(ex.getMessage(), ex);
                } finally {
                    // Clean up the temp
                    FileSystemUtils.deleteRecursively(targetFile.getParentFile());
                }
            });
        } else {

            // Import an individual bag
            executor.submit(() -> {
                try {
                    importService.importZipFile(Paths.get(destination), pid, bagInfos);
                } catch (Exception ex) {
                    // Log the error
                    logger.error(ex.getMessage(), ex);
                } finally {
                    // Clean up the temp
                    FileSystemUtils.deleteRecursively(targetFile.getParentFile());
                }
            });
        }

        //trackingRepository.save(info);

        // Put PID in the Location header
        URI uri = new URI(pid);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uri);

        return ResponseEntity.accepted()
                .headers(headers)
                .body(new ResponseMessage(HttpStatus.ACCEPTED, "Your data is being processed"));
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
