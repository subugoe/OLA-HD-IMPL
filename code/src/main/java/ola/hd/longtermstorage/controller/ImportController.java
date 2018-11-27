package ola.hd.longtermstorage.controller;

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
    public ResponseEntity<String> importData(@RequestParam("file") MultipartFile file) {

        String result = "";

        InputStream fileStream;
        try {
            fileStream = file.getInputStream();
            File targetFile = new File("tmp/" + file.getOriginalFilename());
            FileUtils.copyInputStreamToFile(fileStream, targetFile);

            result = importService.importZipFile(targetFile);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
