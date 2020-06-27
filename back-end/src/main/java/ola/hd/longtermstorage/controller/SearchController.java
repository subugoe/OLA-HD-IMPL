package ola.hd.longtermstorage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import ola.hd.longtermstorage.domain.*;
import ola.hd.longtermstorage.repository.mongo.ArchiveRepository;
import ola.hd.longtermstorage.service.ArchiveManagerService;
import ola.hd.longtermstorage.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Api(description = "This endpoint is used to search in the system.")
@RestController
public class SearchController {

    private final SearchService searchService;
    private final ArchiveManagerService archiveManagerService;
    private final ArchiveRepository archiveRepository;

    @Autowired
    public SearchController(SearchService searchService, ArchiveManagerService archiveManagerService,
                            ArchiveRepository archiveRepository) {
        this.searchService = searchService;
        this.archiveManagerService = archiveManagerService;
        this.archiveRepository = archiveRepository;
    }

    @ApiOperation(value = "Search on archive.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Search success", response = SearchResults.class)
    })
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(@ApiParam(value = "The query used to search.", required = true)
                                    @RequestParam(name = "q")
                                            String query,
                                    @ApiParam(value = "Max returned results.")
                                    @RequestParam(defaultValue = "25")
                                            int limit,
                                    @ApiParam(value = "Scroll ID for pagination")
                                    @RequestParam(defaultValue = "")
                                            String scroll) throws IOException {

        SearchRequest searchRequest = new SearchRequest(query, limit, scroll);

        SearchResults results = searchService.search(searchRequest);

        ObjectMapper mapper = new ObjectMapper();
        for (SearchHit hit : results.getHits()) {
            String data;

            if (hit.getType().equals("file")) {
                data = new String(archiveManagerService.getFile(hit.getId(), hit.getName(), true).getContent());
            } else {
                data = archiveManagerService.getArchiveInfo(hit.getId(), false, 0, 0);
            }

            SearchHitDetail detail = mapper.readValue(data, SearchHitDetail.class);
            hit.setDetail(detail);
        }

        return ResponseEntity.ok(results);
    }

    @ApiOperation(value = "Search for an archive based on its internal ID.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Search success", response = String.class),
            @ApiResponse(code = 404, message = "Archive not found", response = String.class)
    })
    @GetMapping(value = "/search-archive/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchArchive(@ApiParam(value = "Internal ID of the archive.", required = true)
                                           @PathVariable String id,

                                           @ApiParam(value = "An option to include all files in return.")
                                           @RequestParam(defaultValue = "false") boolean withFile,

                                           @ApiParam(value = "How many files should be returned?")
                                           @RequestParam(defaultValue = "1000") int limit,

                                           @ApiParam(value = "How many files should be skipped from the beginning?")
                                           @RequestParam(defaultValue = "0") int offset) throws IOException {

        String info = archiveManagerService.getArchiveInfo(id, withFile, limit, offset);

        return ResponseEntity.ok(info);
    }

    @ApiOperation(value = "Get the information of an archive from the system database.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Information found", response = String.class),
            @ApiResponse(code = 404, message = "Information not found", response = String.class)
    })
    @GetMapping(value = "/search-archive-info/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArchiveResponse> searchArchiveInfo(@ApiParam(value = "Internal ID of the archive.", required = true)
                                                             @PathVariable String id) {
        // Get the data
        Archive archive = archiveRepository.findByOnlineIdOrOfflineId(id, id);

        // Build the response
        ArchiveResponse response = new ArchiveResponse();

        // Set some data
        response.setPid(archive.getPid());
        response.setOnlineId(archive.getOnlineId());
        response.setOfflineId(archive.getOfflineId());

        // Set previous version
        Archive prevArchive = archive.getPreviousVersion();
        if (prevArchive != null) {
            ArchiveResponse prevRes = new ArchiveResponse();
            prevRes.setPid(prevArchive.getPid());
            prevRes.setOnlineId(prevArchive.getOnlineId());
            prevRes.setOfflineId(prevArchive.getOfflineId());

            response.setPreviousVersion(prevRes);
        }

        // Set next versions
        List<Archive> nextVersions = archive.getNextVersions();
        if (nextVersions != null) {
            for (Archive nextArchive : nextVersions) {
                ArchiveResponse nextRes = new ArchiveResponse();
                nextRes.setPid(nextArchive.getPid());
                nextRes.setOnlineId(nextArchive.getOnlineId());
                nextRes.setOfflineId(nextArchive.getOfflineId());

                response.addNextVersion(nextRes);
            }
        }

        return ResponseEntity.ok(response);
    }
}
