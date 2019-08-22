package ola.hd.longtermstorage.controller;

import ola.hd.longtermstorage.domain.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ExceptionHandlerService extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerService.class);

    @ExceptionHandler({HttpClientErrorException.class})
    public ResponseEntity<?> handleClientException(HttpStatusCodeException ex, ServletWebRequest request) {

        // Extract necessary information
        HttpStatus status = ex.getStatusCode();
        String message = ex.getStatusText();
        String uri = request.getRequest().getRequestURI();

        // Return the error message
        return new ResponseEntity<>(new ResponseMessage(status, message, uri), status);
    }

    @ExceptionHandler({Exception.class, HttpServerErrorException.class})
    public ResponseEntity<?> handleException(Exception ex, ServletWebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "A problem occurred while processing your request. Please try again later.";
        String uri = request.getRequest().getRequestURI();

        // Log the error
        logger.error(ex.getMessage(), ex);

        // Let the parent process further
        return handleExceptionInternal(ex, new ResponseMessage(status, message, uri), null, status, request);
    }

    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex, ServletWebRequest request) {

        // Extract necessary information
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = ex.getMessage();
        String uri = request.getRequest().getRequestURI();

        // Return the error message
        return new ResponseEntity<>(new ResponseMessage(status, message, uri), status);
    }
}
