package ola.hd.longtermstorage.service;

import ola.hd.longtermstorage.domain.SearchRequest;
import ola.hd.longtermstorage.domain.SearchResults;

import java.io.IOException;

public interface SearchService {

    /**
     * Used to perform search on the storage
     * @param searchRequest And instance of the {@link SearchRequest} class.
     * @return              The search result.
     * @throws IOException  Thrown when there is a problem with the search.
     */
    SearchResults search(SearchRequest searchRequest) throws IOException;
}
