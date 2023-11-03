package com.example.gateway.commons.dto.card;

import com.example.gateway.integrations.kora.dtos.card.Customer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
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
