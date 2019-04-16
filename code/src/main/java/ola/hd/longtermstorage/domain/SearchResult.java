package ola.hd.longtermstorage.domain;

public class SearchResult {

    private String ppn;
    private String title;

    public SearchResult(String ppn, String title) {
        this.ppn = ppn;
        this.title = title;
    }

    public String getPpn() {
        return ppn;
    }

    public void setPpn(String ppn) {
        this.ppn = ppn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
