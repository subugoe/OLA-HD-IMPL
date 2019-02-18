package ola.hd.longtermstorage.controller;

import ola.hd.longtermstorage.exception.ImportException;
import ola.hd.longtermstorage.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ExportController {

    private final ExportService exportService;

    @Autowired
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping(value = "/export")
    public ResponseEntity<Resource> export(@RequestParam("id") String id) throws IOException, ImportException {
        byte[] data = exportService.export(id);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentLength(data.length)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(resource);
    }
}
