package com.example.gateway.integrations.mastercard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class MasterCardOrder {

    private String id;
    private BigDecimal amount;
    private String currency;
    private String reference;

}
