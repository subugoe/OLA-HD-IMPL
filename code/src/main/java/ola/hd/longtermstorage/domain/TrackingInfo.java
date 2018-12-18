package ola.hd.longtermstorage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Document(collection = "tracking")
public class TrackingInfo {

    @Id
    private String id;

    // Who perform the action
    private String username;

    // What action is it (e.g. upload/create, update, delete)
    private Action action;

    // Name of the affected (uploaded, updated, deleted) file
    private String fileName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date timestamp;

    // Status of the action (e.g. processing, success, failed)
    private Status status;

    // The message goes with the status (e.g. reason of failure)
    private String message;

    // Where the file is stored (e.g. CDSTAR URL)
    private String storageUrl;

    // The PID of the file
    private String pid;

    public TrackingInfo(String username, Action action, String fileName, Date timestamp, Status status) {
        this.username = username;
        this.action = action;
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.status = status;
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

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
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

    public String getStorageUrl() {
        return storageUrl;
    }

    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}

