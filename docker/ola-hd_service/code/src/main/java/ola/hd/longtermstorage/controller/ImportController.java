package ola.hd.longtermstorage.controller;

import ola.hd.longtermstorage.domain.ResponseMessage;
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
import java.util.UUID;

@RestController
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    private ImportService importService;

    @Autowired
    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/bag", produces = "application/json")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalName);

        // Not a zip file
        if (extension == null) {
            return new ResponseEntity<>(
                    new ResponseMessage(415, "The input must be a zip file"),
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        } else if (!extension.equals("zip")) {
            return new ResponseEntity<>(
                    new ResponseMessage(415, "The input must be a zip file"),
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        // TODO: check the file structure?

        // For a unique file name
        UUID uuid = UUID.randomUUID();

        try (InputStream fileStream = file.getInputStream()) {
            File targetFile = new File("tmp/" + uuid + "/" + originalName);
            FileUtils.copyInputStreamToFile(fileStream, targetFile);

            importService.importZipFile(targetFile);

        } catch (IOException e) {
            e.printStackTrace();
            logger.info(e.getMessage(), e);

            return new ResponseEntity<>(
                    new ResponseMessage(500, "Internal Server Error"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new ResponseMessage(200, "Your file was uploaded"), HttpStatus.OK);
    }
}
