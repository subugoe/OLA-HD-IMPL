package ola.hd.longtermstorage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "archive")
public class Archive {

    @Id
    private String id;

    // PID of the archive
    private String pid;

    // CDSTAR-ID of an online archive
    private String onlineId;

    // CDSTAR-ID of an offline archive
    private String offlineId;

    @DBRef(lazy = true)
    private Archive previousVersion;

    @DBRef(lazy = true)
    private List<Archive> nextVersions;

    protected Archive() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }

    public Archive(String pid, String onlineId, String offlineId) {
        this.pid = pid;
        this.onlineId = onlineId;
        this.offlineId = offlineId;
    }

    public void addNextVersion(Archive nextVersion) {
        if (nextVersions == null) {
            nextVersions = new ArrayList<>();
        }
        nextVersions.add(nextVersion);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
