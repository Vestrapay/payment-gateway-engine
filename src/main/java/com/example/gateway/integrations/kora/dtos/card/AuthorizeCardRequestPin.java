package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizeCardRequestPin {
    @JsonProperty("transaction_reference")
    @NotBlank(message = "transaction reference must be provided")
    private String transactionReference;
    @NotNull(message = "authorization must be provided")
    private AuthorizationPin authorization;
}
