package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizeCardRequestPhone {
    @JsonProperty("transaction_reference")
    private String transactionReference;
    private AuthorizationPhone authorization;
    private String currency = "NGN";


}
