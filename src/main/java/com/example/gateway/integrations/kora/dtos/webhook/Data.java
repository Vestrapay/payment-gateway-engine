package com.example.gateway.integrations.kora.dtos.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Data {
    private String reference;
    @JsonProperty("payment_reference")
    private String paymentReference;
    private String currency;
    private BigDecimal amount;
    private BigDecimal fee;
    private String status;
    @JsonProperty("payment_method")
    private String paymentMethod;
}
