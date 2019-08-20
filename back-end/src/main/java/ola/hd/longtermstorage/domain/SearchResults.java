package ola.hd.longtermstorage.domain;

import java.util.List;

public class SearchResults {

    private int count;

    private int total;

    private String scroll;

    private List<SearchHit> hits;

    public SearchResults() {
        // A default constructor is necessary for JSON deserialization
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getScroll() {
        return scroll;
    }

    public void setScroll(String scroll) {
        this.scroll = scroll;
    }

    public List<SearchHit> getHits() {
        return hits;
    }

    public void setHits(List<SearchHit> hits) {
        this.hits = hits;
    }
}
