package com.example.gateway.commons.settlements.dtos;


import com.example.gateway.integrations.kora.dtos.transfer.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class KoraSettlementResponse {
    private boolean status;
    private String message;
    private Data data;
}
