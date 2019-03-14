package ola.hd.longtermstorage.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.http.HttpStatus;

@ApiModel(description = "The response from the server")
public class ResponseMessage {

    @ApiModelProperty(value = "The HTTP status code", example = "201")
    private int httpCode;

    @ApiModelProperty(value = "The HTTP status", example = "CREATED")
    private HttpStatus httpStatus;

    @ApiModelProperty(value = "The message used to provide more information",
            example = "Your data has been uploaded.")
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
