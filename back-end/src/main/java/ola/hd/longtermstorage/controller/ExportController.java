package ola.hd.longtermstorage.controller;

import io.swagger.annotations.*;
import ola.hd.longtermstorage.domain.ArchiveStatus;
import ola.hd.longtermstorage.domain.ExportRequest;
import ola.hd.longtermstorage.domain.ResponseMessage;
import ola.hd.longtermstorage.repository.mongo.ExportRequestRepository;
import ola.hd.longtermstorage.service.ArchiveManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
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
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> export(@ApiParam(value = "The PID or the PPN of the work.", required = true)
                                               @RequestParam("id") String id) throws IOException {
        byte[] data = archiveManagerService.export(id, "quick");
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentLength(data.length)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(resource);
    }

    @ApiOperation(value = "Send a request to export data on tapes")
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

    @ApiOperation(value = "Export the cold archive which was already moved to the hard drive.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "An archive with the specified identifier was found.",
                    response = byte[].class),
            @ApiResponse(code = 404, message = "An archive with the specified identifier was not found.",
                    response = ResponseMessage.class),
            @ApiResponse(code = 409, message = "The archive is still on tape. A full export request must be made first.",
                    response = ResponseMessage.class)
    })
    @GetMapping(value = "/full-export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> fullExport(@ApiParam(value = "The PID or the PPN of the work.", required = true)
                                                   @RequestParam("id") String id) throws IOException {

        byte[] data = archiveManagerService.export(id, "full");
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentLength(data.length)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(resource);
    }
}
