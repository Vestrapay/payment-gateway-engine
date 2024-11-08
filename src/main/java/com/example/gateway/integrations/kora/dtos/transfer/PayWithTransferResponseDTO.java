package com.example.gateway.integrations.kora.dtos.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayWithTransferResponseDTO {
    private boolean status;
    private String message;
    private Data data;
}
