package ola.hd.longtermstorage.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ArchiveResponse {

    // PID of the archive
    private String pid;

    // CDSTAR-ID of an online archive
    private String onlineId;

    // CDSTAR-ID of an offline archive
    private String offlineId;

    private Archive previousVersion;

    private List<Archive> nextVersions;

    public ArchiveResponse() {
        // A default constructor is necessary for JSON deserialization
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
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

    public Archive getPreviousVersion() {
        return previousVersion;
    }

    public void setPreviousVersion(Archive previousVersion) {
        this.previousVersion = previousVersion;
    }

    public List<Archive> getNextVersions() {
        return nextVersions;
    }

    public void setNextVersions(List<Archive> nextVersions) {
        this.nextVersions = nextVersions;
    }
}
