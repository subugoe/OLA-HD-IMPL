package ola.hd.longtermstorage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Document(collection = "exportRequest")
public class ExportRequest {

    @Id
    private String id;

    // Who send the request
    private String username;

    // PID of the archive
    private String pid;

    private Instant timestamp;

    private ArchiveStatus status;

    // Time when the archive should be deleted from the hard drive
    private Instant availableUntil;

    protected ExportRequest() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }

    public ExportRequest(String username, String pid, ArchiveStatus status) {
        this.username = username;
        this.pid = pid;
        this.status = status;
        this.timestamp = Instant.now();

        // An archive will be available on hard drive for at least 7 days
        this.availableUntil = this.timestamp.plus(7, ChronoUnit.DAYS);
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

    public ArchiveStatus getStatus() {
        return status;
    }

    public void setStatus(ArchiveStatus status) {
        this.status = status;
    }

    public Instant getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(Instant availableUntil) {
        this.availableUntil = availableUntil;
    }
}
