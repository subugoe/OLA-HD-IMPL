package ola.hd.longtermstorage.service;

import ola.hd.longtermstorage.domain.SearchRequest;
import ola.hd.longtermstorage.domain.SearchResult;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public interface SearchService {
    List<SearchResult> search(SearchRequest searchRequest) throws IOException, ParserConfigurationException, SAXException;
}
