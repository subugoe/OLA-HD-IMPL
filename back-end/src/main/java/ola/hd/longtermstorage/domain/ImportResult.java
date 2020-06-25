package ola.hd.longtermstorage.domain;

import java.util.AbstractMap;
import java.util.List;

public class ImportResult {

    private String onlineId;
    private String offlineId;
    private List<AbstractMap.SimpleImmutableEntry<String, String>> metaData;

    public ImportResult(String onlineId, String offlineId, List<AbstractMap.SimpleImmutableEntry<String, String>> metaData) {
        this.onlineId = onlineId;
        this.offlineId = offlineId;
        this.metaData = metaData;
    }

    public String getOnlineId() {
        return onlineId;
    }

    public void setOnlineId(String onlineId) {
        this.onlineId = onlineId;
    }

    public String getOfflineId() {
        return offlineId;
    }

    public void setOfflineId(String offlineId) {
        this.offlineId = offlineId;
    }

    public List<AbstractMap.SimpleImmutableEntry<String, String>> getMetaData() {
        return metaData;
    }

    public void setMetaData(List<AbstractMap.SimpleImmutableEntry<String, String>> metaData) {
        this.metaData = metaData;
    }
}
