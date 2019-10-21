package ola.hd.longtermstorage.controller;

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

@RestController
public class SearchController {

    private final SearchService searchService;

    private final ArchiveManagerService archiveManagerService;

    @Autowired
    public SearchController(SearchService searchService, ArchiveManagerService archiveManagerService) {
        this.searchService = searchService;
        this.archiveManagerService = archiveManagerService;
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(@RequestParam(name = "q") String query, @RequestParam(defaultValue = "1000") int limit) throws IOException {

        SearchRequest searchRequest = new SearchRequest(query, limit);

        SearchResults results = searchService.search(searchRequest);

        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/search-archive/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchArchive(@PathVariable String id, @RequestParam(defaultValue = "false") boolean withFile) throws IOException {

        String info = archiveManagerService.getArchiveInfo(id, withFile);

        return ResponseEntity.ok(info);
    }
}
