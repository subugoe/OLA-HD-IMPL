package ola.hd.longtermstorage.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.http.HttpStatus;

@ApiModel(description = "The response from the server")
public class ResponseMessage {

    @ApiModelProperty(notes = "The HTTP status code")
    private int httpCode;

    @ApiModelProperty(notes = "The HTTP status")
    private HttpStatus httpStatus;

    @ApiModelProperty(notes = "The message used to provide more information")
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
