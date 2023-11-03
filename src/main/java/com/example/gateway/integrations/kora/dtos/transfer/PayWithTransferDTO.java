package com.example.gateway.integrations.kora.dtos.transfer;

import com.example.gateway.integrations.kora.dtos.card.Customer;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayWithTransferDTO {
    private String reference;
    private BigDecimal amount;
    private String currency;
    @JsonProperty("notification_url")
    private String notificationUrl;
    private Customer customer;
}
