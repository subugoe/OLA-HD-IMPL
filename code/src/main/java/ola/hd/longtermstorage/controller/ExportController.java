package ola.hd.longtermstorage.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import ola.hd.longtermstorage.domain.ResponseMessage;
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

@Api(description = "This endpoint is used to export data from the system.")
@RestController
public class ExportController {

    private final ExportService exportService;

    @Autowired
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @ApiOperation(value = "Quickly export a ZIP file via PID. This ZIP file only contains files stored on hard disks.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "An archive with the specified identifier was found.",
                    response = byte[].class),
            @ApiResponse(code = 404, message = "An archive with the specified identifier was not found.",
                    response = ResponseMessage.class)
    })
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> export(@RequestParam("id") String id) throws IOException, ImportException {
        byte[] data = exportService.export(id);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentLength(data.length)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(resource);
    }
}
