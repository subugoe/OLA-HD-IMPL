package ola.hd.longtermstorage.domain;

public class SearchRequest {

    private String query;
    private int limit;

    public SearchRequest(String query) {
        this.query = query;
        this.limit = 1000;
    }

    public SearchRequest(String query, int limit) {
        this.query = query;
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
}
