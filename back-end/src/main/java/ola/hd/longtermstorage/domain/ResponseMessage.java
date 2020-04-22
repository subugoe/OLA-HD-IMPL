package ola.hd.longtermstorage.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@ApiModel(description = "The response from the server")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseMessage {

    @ApiModelProperty(value = "The HTTP status code")
    private int httpCode;

    @ApiModelProperty(value = "The HTTP status")
    private HttpStatus httpStatus;

    @ApiModelProperty(value = "The message used to provide more information")
    private String message;

    @ApiModelProperty(value = "PID of the uploaded data")
    private String pid;

    @ApiModelProperty(value = "The time when the request was made")
    private Instant timestamp;

    @ApiModelProperty(value = "The address where the request was sent to")
    private String path;

    public ResponseMessage(HttpStatus httpStatus, String message, String path) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
        this.httpCode = httpStatus.value();
    }

    public ResponseMessage(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.httpCode = httpStatus.value();
        this.timestamp = Instant.now();
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

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }
}
