package com.example.gateway.integrations.kora.dtos.transfer;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayWithTransferResponseDTO {
    private boolean status;
    private String message;
    private Data data;
}
