package ola.hd.longtermstorage.controller;

import ola.hd.longtermstorage.domain.ResponseMessage;
import ola.hd.longtermstorage.service.ImportService;
import org.apache.commons.io.FileUtils;
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

@RestController
public class ImportController {

    private ImportService importService;

    @Autowired
    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/bag", produces = "application/json")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file) {

        String result = "";

        try (InputStream fileStream = file.getInputStream()) {
            File targetFile = new File("tmp/" + file.getOriginalFilename());
            FileUtils.copyInputStreamToFile(fileStream, targetFile);

            result = importService.importZipFile(targetFile);

        } catch (IOException e) {
            e.printStackTrace();

            return new ResponseEntity<>(new ResponseMessage(500, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
