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

    // Who perform the action
    private String username;

    //@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant timestamp;

    // TrackingStatus of the action (e.g. processing, success, failed)
    private TrackingStatus status;

    // The message goes with the status (e.g. reason of failure)
    private String message;

    // The PID of the file
    private String pid;

    protected TrackingInfo() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }

    public TrackingInfo(String username, TrackingStatus status, String message, String pid) {
        this.username = username;
        this.status = status;
        this.message = message;
        this.pid = pid;
        this.timestamp = Instant.now();
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

    public TrackingStatus getStatus() {
        return status;
    }

    public void setStatus(TrackingStatus status) {
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
}

