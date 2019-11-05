package ola.hd.longtermstorage.domain;

public class SearchRequest {

    private String query;
    private int limit;
    private String scroll;

    public SearchRequest(String query, int limit, String scroll) {
        this.query = query;
        this.scroll = scroll;
        setLimit(limit);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {

        // Max number of return results
        if (limit > 1000) {
            limit = 1000;
        }
        this.limit = limit;
    }

    public String getScroll() {
        return scroll;
    }

    public void setScroll(String scroll) {
        this.scroll = scroll;
    }
}
