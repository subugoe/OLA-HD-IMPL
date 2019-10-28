package ola.hd.longtermstorage.controller;

import io.swagger.annotations.*;
import okhttp3.Response;
import ola.hd.longtermstorage.domain.ArchiveStatus;
import ola.hd.longtermstorage.domain.ExportRequest;
import ola.hd.longtermstorage.domain.ResponseMessage;
import ola.hd.longtermstorage.repository.mongo.ExportRequestRepository;
import ola.hd.longtermstorage.service.ArchiveManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.concurrent.atomic.AtomicLong;

@Api(description = "This endpoint is used to export data from the system.")
@RestController
public class ExportController {

    private final ArchiveManagerService archiveManagerService;

    private final ExportRequestRepository exportRequestRepository;

    @Autowired
    public ExportController(ArchiveManagerService archiveManagerService, ExportRequestRepository exportRequestRepository) {
        this.archiveManagerService = archiveManagerService;
        this.exportRequestRepository = exportRequestRepository;
    }

    @ApiOperation(value = "Quickly export a ZIP file via PID. This ZIP file only contains files stored on hard disks.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "An archive with the specified identifier was found.",
                    response = byte[].class),
            @ApiResponse(code = 404, message = "An archive with the specified identifier was not found.",
                    response = ResponseMessage.class)
    })
    @GetMapping(value = "/export", produces = {
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    public ResponseEntity<StreamingResponseBody> export(@ApiParam(value = "The PID or the PPN of the work.", required = true)
                                           @RequestParam("id") String id) {
        return exportData(id, "quick");
    }

    @ApiOperation(value = "Send a request to export data on tapes.",
            authorizations = {
                    @Authorization(value = "basicAuth"),
                    @Authorization(value = "bearer")
            })
    @ApiResponses({
            @ApiResponse(code = 200, message = "The archive is already on the hard drive.",
                    response = byte[].class),
            @ApiResponse(code = 202, message = "Request accepted. Data is being transfer from tape to hard drive.",
                    response = byte[].class),
            @ApiResponse(code = 404, message = "An archive with the specified identifier was not found.",
                    response = ResponseMessage.class)
    })
    @GetMapping(value = "/export-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> fullExportRequest(@ApiParam(value = "The PID or the PPN of the work.", required = true)
                                               @RequestParam("id") String id,
                                               @ApiIgnore Principal principal) throws IOException {

        // Move the archive from tape to disk
        archiveManagerService.moveFromTapeToDisk(id);

        // Save the request info to the database
        ExportRequest exportRequest = new ExportRequest(principal.getName(), id, ArchiveStatus.PENDING);
        exportRequestRepository.save(exportRequest);

        return ResponseEntity.accepted()
                .body(new ResponseMessage(HttpStatus.ACCEPTED, "Your request is being processed."));
    }

    @ApiOperation(value = "Export the cold archive which was already moved to the hard drive.",
            authorizations = {
                    @Authorization(value = "basicAuth"),
                    @Authorization(value = "bearer")
            })
    @ApiResponses({
            @ApiResponse(code = 200, message = "An archive with the specified identifier was found.",
                    response = byte[].class),
            @ApiResponse(code = 404, message = "An archive with the specified identifier was not found.",
                    response = ResponseMessage.class),
            @ApiResponse(code = 409, message = "The archive is still on tape. A full export request must be made first.",
                    response = ResponseMessage.class)
    })
    @GetMapping(value = "/full-export", produces = {
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    public ResponseEntity<StreamingResponseBody> fullExport(@ApiParam(value = "The PID or the PPN of the work.", required = true)
                                               @RequestParam("id") String id) {

        return exportData(id, "full");
    }

    private ResponseEntity<StreamingResponseBody> exportData(String id, String type) {

        // Set proper file name
        String contentDisposition = "attachment;filename=";
        if (type.equals("quick")) {
            contentDisposition += "quick-export.zip";
        } else {
            contentDisposition += "full-export.zip";
        }

        // Build the response stream
        StreamingResponseBody stream = outputStream -> {

            try (Response response = archiveManagerService.export(id, type)) {
                if (response.body() != null) {
                    InputStream inputStream = response.body().byteStream();

                    int numberOfBytesToWrite;
                    byte[] data = new byte[1024];

                    while ((numberOfBytesToWrite = inputStream.read(data, 0, data.length)) != -1) {
                        outputStream.write(data, 0, numberOfBytesToWrite);
                    }
                }
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(stream);
    }
}
