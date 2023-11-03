package com.example.gateway.integrations.wemabank.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class WemaTokenResponse {
    private int id;
    private Object firstName;
    private Object lastName;
    private String username;
    private Object password;
    private String token;
    private String refreshToken;
    private String refreshTokenExpires;
}
