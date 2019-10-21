package ola.hd.longtermstorage.controller;

import io.swagger.annotations.*;
import ola.hd.longtermstorage.domain.SearchRequest;
import ola.hd.longtermstorage.domain.SearchResults;
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

@Api(description = "This endpoint is used to search in the system.")
@RestController
public class SearchController {

    private final SearchService searchService;

    private final ArchiveManagerService archiveManagerService;

    @Autowired
    public SearchController(SearchService searchService, ArchiveManagerService archiveManagerService) {
        this.searchService = searchService;
        this.archiveManagerService = archiveManagerService;
    }

    @ApiOperation(value = "Search on archive.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Search success", response = SearchResults.class)
    })
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(@ApiParam(value = "The query used to search.", required = true)
                                        @RequestParam(name = "q") String query,
                                    @RequestParam(defaultValue = "1000") int limit) throws IOException {

        SearchRequest searchRequest = new SearchRequest(query, limit);

        SearchResults results = searchService.search(searchRequest);

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
                                           @RequestParam(defaultValue = "false") boolean withFile) throws IOException {

        String info = archiveManagerService.getArchiveInfo(id, withFile);

        return ResponseEntity.ok(info);
    }
}
