package ola.hd.longtermstorage.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackingResponse {

    private TrackingInfo trackingInfo;
    private ArchiveResponse archiveResponse;

    public TrackingResponse(TrackingInfo trackingInfo) {
        this.trackingInfo = trackingInfo;
    }

    public TrackingResponse(TrackingInfo trackingInfo, ArchiveResponse archiveResponse) {
        this.trackingInfo = trackingInfo;
        this.archiveResponse = archiveResponse;
    }

    public TrackingInfo getTrackingInfo() {
        return trackingInfo;
    }

    public void setTrackingInfo(TrackingInfo trackingInfo) {
        this.trackingInfo = trackingInfo;
    }

    public ArchiveResponse getArchiveResponse() {
        return archiveResponse;
    }

    public void setArchiveResponse(ArchiveResponse archiveResponse) {
        this.archiveResponse = archiveResponse;
    }
}
