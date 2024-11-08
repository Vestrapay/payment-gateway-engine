package com.example.gateway.commons.settlements.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KoraSettlementRequest {
    private String reference;
    private Destination destination;
}
