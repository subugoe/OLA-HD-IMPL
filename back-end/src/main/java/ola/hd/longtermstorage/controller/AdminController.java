package ola.hd.longtermstorage.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import ola.hd.longtermstorage.domain.*;
import ola.hd.longtermstorage.repository.mongo.ArchiveRepository;
import ola.hd.longtermstorage.repository.mongo.TrackingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Api(description = "This endpoint is used to get information for administration purposes.")
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final TrackingRepository trackingRepository;
    private final ArchiveRepository archiveRepository;

    @Autowired
    public AdminController(TrackingRepository trackingRepository, ArchiveRepository archiveRepository) {
        this.trackingRepository = trackingRepository;
        this.archiveRepository = archiveRepository;
    }

    @ApiOperation(value = "Get information about import process.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Query success", response = TrackingInfo[].class)
    })
    @GetMapping(value = "/import-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TrackingResponse>> GetImportData(String username, int page, int limit) {
        List<TrackingInfo> trackingInfos = trackingRepository.findByUsername(username, PageRequest.of(page, limit, Sort.Direction.DESC, "timestamp"));

        List<TrackingResponse> results = new ArrayList<>();

        TrackingResponse trackingResponse;
        for (TrackingInfo trackingInfo : trackingInfos) {

            // With each successful import
            if (trackingInfo.getStatus() == TrackingStatus.SUCCESS) {

                // Get more info (version, online/offline ID in CDSTAR...)
                Archive archive = archiveRepository.findByPid(trackingInfo.getPid());
                ArchiveResponse archiveResponse = new ArchiveResponse();
                archiveResponse.setPid(archive.getPid());
                archiveResponse.setOnlineId(archive.getOnlineId());
                archiveResponse.setOfflineId(archive.getOfflineId());

                trackingResponse = new TrackingResponse(trackingInfo, archiveResponse);
            } else {
                trackingResponse = new TrackingResponse(trackingInfo);
            }
            results.add(trackingResponse);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(results);
    }
}
