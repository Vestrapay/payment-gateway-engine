package com.example.gateway.integrations.kora.dtos.transfer;

import com.example.gateway.integrations.kora.dtos.card.Customer;
import com.example.gateway.integrations.kora.dtos.card.MetaData;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayWithTransferDTO {
    private String reference;
    private BigDecimal amount;
    private String currency;
    @JsonProperty("notification_url")
    private String notificationUrl;
    private Customer customer;
    @JsonIgnore
    private String metaData;
}
