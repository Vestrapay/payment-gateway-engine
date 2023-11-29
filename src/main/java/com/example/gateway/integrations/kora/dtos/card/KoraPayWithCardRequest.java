package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class KoraPayWithCardRequest {
    private String reference;
    private Card2 card;
    private BigDecimal amount;
    private String currency;
    @JsonProperty("redirect_url")
    private String redirectUrl;
    private Customer customer;
    private MetaData2 metadata;
}
