package com.example.gateway.integrations.paymentlink.utils;

import com.example.gateway.commons.keys.models.Keys;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseDTO<T> {
    private String message;
    private HttpStatus status;
    private int statusCode;
    private T data;
    private Keys keys;
    private List<String> errors;
}
