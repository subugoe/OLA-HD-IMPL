package ola.hd.longtermstorage.domain;

import org.springframework.http.HttpStatus;

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
