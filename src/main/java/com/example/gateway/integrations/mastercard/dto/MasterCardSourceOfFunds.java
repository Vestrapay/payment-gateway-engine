package com.example.gateway.integrations.mastercard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class MasterCardSourceOfFunds {

    private String type;
    private ProvidedDto provided;
}
