package ola.hd.longtermstorage.domain;

import org.springframework.http.HttpHeaders;

public class HttpFile {

    private byte[] content;

    private HttpHeaders headers;

    public HttpFile(byte[] content) {
        this.content = content;
        this.headers = new HttpHeaders();
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void addHeaders(String key, String value) {
        headers.add(key, value);
    }
}
