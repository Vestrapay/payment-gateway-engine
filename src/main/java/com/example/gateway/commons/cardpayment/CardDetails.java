package com.example.gateway.commons.cardpayment;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class CardDetails {

    @NotBlank(message = "AuthDataVersion is required")
    private String authDataVersion;

    @NotBlank(message = "pan is required")
    private String pan;

    @NotBlank(message = "expiryDate is required")
    private String expiryDate;

    @NotBlank(message = "cvv2 is required")
    private String cvv2;

    private String pin;
}
