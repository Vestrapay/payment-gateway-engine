package com.example.gateway.integrations.kora.dtos.card;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class EncryptDecryptRequest {
    @NotBlank
    private String key;
    @NotBlank
    private String body;
}
