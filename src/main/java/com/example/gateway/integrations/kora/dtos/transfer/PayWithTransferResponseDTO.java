package com.example.gateway.integrations.kora.dtos.transfer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PayWithTransferResponseDTO {
    private boolean status;
    private String message;
    private Data data;
}
