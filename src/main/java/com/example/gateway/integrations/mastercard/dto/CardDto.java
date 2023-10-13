package com.example.gateway.integrations.mastercard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CardDto {

    private ExpiryDto expiry;
    private String number;
    private String securityCode;

}
