package ola.hd.longtermstorage.exception;

public class ImportException extends Exception {

    private int httpStatusCode;
    private String httpMessage;

    public ImportException() {
    }

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImportException(Throwable cause) {
        super(cause);
    }

    public ImportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public void setHttpMessage(String httpMessage) {
        this.httpMessage = httpMessage;
    }

    @Override
    public String getMessage() {
        return String.format("%s. %d - %s", super.getMessage(), httpStatusCode, httpMessage);
    }
}
