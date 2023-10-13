package com.example.gateway.commons.utils;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Response<T> {
    private String message;
    private HttpStatus status;
    private int statusCode;
    private T data;
    private List<String> errors;
}
