package com.example.gateway.integrations.budpay.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentRequestDto {
    private String amount;
    private String card;
    private String currency;
    private String email;
    private String reference;
    private boolean enforceSecureAuth = true;

}
