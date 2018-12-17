package ola.hd.longtermstorage.domain;

public class ResponseMessage {

    private int code;

    private String message;

    public ResponseMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
