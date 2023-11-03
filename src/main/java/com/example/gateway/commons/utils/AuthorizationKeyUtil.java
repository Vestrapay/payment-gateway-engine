package com.example.gateway.commons.utils;

import com.example.gateway.commons.exceptions.CustomException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizationKeyUtil {
    public static String extractKey(String authorizationHeader){
        if (authorizationHeader.isEmpty())
            throw new CustomException(Response.builder()
                    .errors(List.of("authorization header must be present"))
                    .message("Failed")
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .status(HttpStatus.BAD_REQUEST)
                    .build(), HttpStatus.BAD_REQUEST);

        String vestraPayMerchantSecretKey = authorizationHeader.substring(7);
        if (vestraPayMerchantSecretKey.isEmpty())
            throw new CustomException(Response.builder()
                    .errors(List.of("secret key in header must be present"))
                    .message("Failed")
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .status(HttpStatus.BAD_REQUEST)
                    .build(), HttpStatus.BAD_REQUEST);

        return vestraPayMerchantSecretKey;
    }
}
