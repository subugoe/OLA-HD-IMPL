package ola.hd.longtermstorage.domain;

import org.springframework.http.HttpStatus;

public class ResponseMessage {

    private int httpCode;
    private HttpStatus httpStatus;
    private String message;

    public ResponseMessage(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.httpCode = httpStatus.value();
    }

    public int getHttpCode() {
        return httpCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
