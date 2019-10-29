package ola.hd.longtermstorage.domain;

public class DownloadRequest {

    private String archiveId;

    private String[] files;

    public DownloadRequest() {
    }

    public String getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(String archiveId) {
        this.archiveId = archiveId;
    }

    public String[] getFiles() {
        return files;
    }

    public void setFiles(String[] files) {
        this.files = files;
    }
}
