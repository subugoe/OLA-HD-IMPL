package ola.hd.longtermstorage.controller;

import io.swagger.annotations.*;
import okhttp3.Response;
import ola.hd.longtermstorage.domain.*;
import ola.hd.longtermstorage.repository.mongo.ExportRequestRepository;
import ola.hd.longtermstorage.service.ArchiveManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;

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
    public ResponseEntity<StreamingResponseBody> export(@ApiParam(value = "The ID of the work.", required = true)
                                                        @RequestParam String id,
                                                        @ApiParam(value = "Is this an internal ID or not (PID, PPN).", required = true)
                                                        @RequestParam(defaultValue = "false") boolean isInternal) {
        return exportData(id, "quick", isInternal);
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
                                               @RequestParam String id,
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
                                                            @RequestParam String id,
                                                            @ApiParam(value = "Is this an internal ID or not (PID, PPN).", required = true)
                                                            @RequestParam(defaultValue = "false") boolean isInternal) {

        return exportData(id, "full", isInternal);
    }

    @PostMapping(value = "/download", consumes = MediaType.APPLICATION_JSON_VALUE, produces = {
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    public ResponseEntity<StreamingResponseBody> downloadFiles(@RequestBody DownloadRequest payload) {

        // Set proper header
        String contentDisposition = "attachment;filename=download.zip";

        // Build the response stream
        StreamingResponseBody stream = outputStream -> archiveManagerService.downloadFiles(payload.getArchiveId(), payload.getFiles(), outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(stream);
    }

    @GetMapping(value = "/download-file/{id}", produces = {
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.APPLICATION_OCTET_STREAM_VALUE
    })
    public ResponseEntity<Resource> downloadFile(@ApiParam(value = "Internal ID of the archive.", required = true)
                                                 @PathVariable String id,
                                                 @ApiParam(value = "Path to the requested file", required = true)
                                                 @RequestParam String path) throws IOException {

        HttpFile httpFile = archiveManagerService.getFile(id, path, false);
        ByteArrayResource resource = new ByteArrayResource(httpFile.getContent());

        HttpHeaders headers = httpFile.getHeaders();

        // Inline content-disposition: render the file directly on the browser if possible
        String contentDisposition = "inline";

        // Get proper content-type, or use application/octet-stream by default.
        // Without a proper content-type, the browser cannot display the file correctly.
        String contentType = headers.getContentType() != null ? headers.getContentType().toString() : "application/octet-stream";

        // Set charset
        contentType += ";charset=utf-8";

        long contentLength = headers.getContentLength();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_LENGTH, contentLength + "")
                .body(resource);
    }

    private ResponseEntity<StreamingResponseBody> exportData(String id, String type, boolean isInternal) {

        // Set proper file name
        String contentDisposition = "attachment;filename=";
        String fileName = "quick-export.zip";

        if (type != null && type.equals("full")) {
            fileName = "full-export.zip";
        }
        contentDisposition += fileName;

        // Build the response stream
        StreamingResponseBody stream = outputStream -> {

            try (Response response = archiveManagerService.export(id, type, isInternal)) {
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
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(stream);
    }
}
