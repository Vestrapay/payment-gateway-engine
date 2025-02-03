package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizeCardRequestAVS {
    @JsonProperty("transaction_reference")
    private String transactionReference;
    private AuthorizationAVS authorization;
    private String currency = "NGN";

}
