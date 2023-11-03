package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class KoraPayWithCardRequest {
    private String reference;
    private Card card;
    private BigDecimal amount;
    private String currency;
    @JsonProperty("redirect_url")
    private String redirectUrl;
    private Customer customer;
    private MetaData metadata;
}
