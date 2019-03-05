package ola.hd.longtermstorage.controller;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import net.lingala.zip4j.core.ZipFile;
import ola.hd.longtermstorage.component.ExecutorWrapper;
import ola.hd.longtermstorage.domain.ResponseMessage;
import ola.hd.longtermstorage.repository.TrackingRepository;
import ola.hd.longtermstorage.service.ImportService;
import ola.hd.longtermstorage.service.PidService;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @PostMapping(value = "/bag", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importData(HttpServletRequest request) throws Exception {

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(HttpStatus.BAD_REQUEST,
                            "The request must be multipart request"));
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
                    return ResponseEntity.badRequest()
                            .body(new ResponseMessage(HttpStatus.BAD_REQUEST,
                                    "Only 1 zip file is allow"));
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

        // Extract the zip file
        ZipFile zipFile = new ZipFile(targetFile);
        zipFile.extractAll(destination);

        // Validate the bag
        Path rootDir = Paths.get(destination);
        BagReader reader = new BagReader();

        // Create a bag from an existing directory
        Bag bag = reader.read(rootDir);

        BagVerifier verifier = new BagVerifier();

        if (BagVerifier.canQuickVerify(bag)) {
            BagVerifier.quicklyVerify(bag);
        }

        // Check for the validity and completeness of a bag
        verifier.isValid(bag, true);

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
            // TODO: Import a new version of a bag
            System.out.println("Importing a new version");
        } else {

            // Import an individual bag
            executor.submit(() -> {
                try {
                    importService.importZipFile(Paths.get(destination), pid, bagInfos);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // Clean up the temp
                    FileSystemUtils.deleteRecursively(targetFile.getParentFile());
                }
            });
        }

        //trackingRepository.save(info);

        URI uri = new URI(pid);
        return ResponseEntity.created(uri)
                .body(new ResponseMessage(HttpStatus.CREATED, "Your data is being processed"));
    }
}
