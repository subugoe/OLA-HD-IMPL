package ola.hd.longtermstorage.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import ola.hd.longtermstorage.domain.TrackingInfo;
import ola.hd.longtermstorage.repository.mongo.TrackingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(description = "This endpoint is used to get information for administration purposes.")
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final TrackingRepository trackingRepository;

    @Autowired
    public AdminController(TrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
    }

    @ApiOperation(value = "Get information about import process.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Query success", response = TrackingInfo[].class)
    })
    @GetMapping(value = "/import-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TrackingInfo>> GetImportData(String username, int page, int limit) {
        List<TrackingInfo> results = trackingRepository.findByUsername(username, PageRequest.of(page, limit, Sort.Direction.DESC, "timestamp"));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(results);
    }
}
