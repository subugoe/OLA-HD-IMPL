package ola.hd.longtermstorage.domain;

public class ImportResult {

    private String onlineUrl;
    private String offlineUrl;

    public ImportResult(String onlineUrl, String offlineUrl) {
        this.onlineUrl = onlineUrl;
        this.offlineUrl = offlineUrl;
    }

    public String getOnlineUrl() {
        return onlineUrl;
    }

    public String getOfflineUrl() {
        return offlineUrl;
    }
}
