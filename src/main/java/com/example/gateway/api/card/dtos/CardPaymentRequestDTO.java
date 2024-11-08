package com.example.gateway.api.card.dtos;

import com.example.gateway.integrations.kora.dtos.card.Customer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardPaymentRequestDTO {
    @NotBlank(message = "transaction reference must be provided")
    private String transactionReference;
    private BigDecimal amount;
    private String currency;
    private Card card;
    private Map<String, Object> metaData;
    private Customer customerDetails;
}
