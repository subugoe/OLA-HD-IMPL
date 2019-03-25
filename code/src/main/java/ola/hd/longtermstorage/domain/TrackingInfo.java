package ola.hd.longtermstorage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "tracking")
public class TrackingInfo {

    @Id
    private String id;

    // TODO: Maybe a @DBRef is needed here later
    // Who perform the action
    private String username;

    //@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant timestamp;

    // Status of the action (e.g. processing, success, failed)
    private Status status;

    // The message goes with the status (e.g. reason of failure)
    private String message;

    // The PID of the file
    private String pid;

    // PID of the previous version
    private String previousVersion;

    // PID of the previous version
    private List<String> nextVersion;

    protected TrackingInfo() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }

    public TrackingInfo(String username, Status status, String message, String pid) {
        this.username = username;
        this.status = status;
        this.message = message;
        this.pid = pid;
        this.timestamp = Instant.now();
    }

    public void addNextVersion(String nextVersion) {
        if (this.nextVersion == null) {
            this.nextVersion = new ArrayList<>();
        }
        this.nextVersion.add(nextVersion);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public void setPreviousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
    }

    public List<String> getNextVersion() {
        return nextVersion;
    }

    public void setNextVersion(List<String> nextVersion) {
        this.nextVersion = nextVersion;
    }
}

