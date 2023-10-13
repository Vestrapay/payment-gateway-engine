package com.example.gateway.commons.exceptions;

import com.example.gateway.commons.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public final ResponseEntity<Response> handleError(CustomException ex) {

        logError(ex);
        return new ResponseEntity<>(ex.getResponse(), ex.getHttpStatus());
    }



    @ExceptionHandler(TimeoutException.class)
    public final ResponseEntity<Response> handleError(TimeoutException ex) {
        logError(ex);
        return new ResponseEntity<>(getResponse(ex,HttpStatus.REQUEST_TIMEOUT), HttpStatus.REQUEST_TIMEOUT);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public final ResponseEntity<Response> handleError(IllegalArgumentException ex) {

        logError(ex);
        return new ResponseEntity<>(getResponse(ex,HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public final ResponseEntity<Response> handleError(MissingServletRequestParameterException ex) {

        logError(ex);
        return new ResponseEntity<>(getResponse(ex,HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public final ResponseEntity<Response> handleError(MissingRequestHeaderException ex) {

        logError(ex);
        return new ResponseEntity<>(getResponse(ex,HttpStatus.BAD_REQUEST), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ArithmeticException.class)
    public final ResponseEntity<Response> handleError(ArithmeticException ex) {

        logError(ex);
        return new ResponseEntity<>(getResponse(ex,HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public final ResponseEntity<Response> handleError(HttpMessageNotReadableException ex) {

        logError(ex);
        return new ResponseEntity<>(getResponse(ex,HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<Response> handleError(Exception ex) {

        logError(ex);
        return new ResponseEntity<>(getResponse(ex,HttpStatus.SERVICE_UNAVAILABLE), HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void logError(Exception ex) {
        log.info("Exception thrown :: {}, Message :: {}", ex.getClass().getSimpleName(), ex.getLocalizedMessage());
        ex.printStackTrace();
    }

    private Response getResponse(Exception ex, HttpStatus httpStatus) {

        return Response.builder()
                .message(httpStatus.getReasonPhrase())
                .statusCode(httpStatus.value())
                .errors(List.of(ex.getMessage()))
                .build();
    }


}
