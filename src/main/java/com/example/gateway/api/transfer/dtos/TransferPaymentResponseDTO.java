package com.example.gateway.api.transfer.dtos;

import com.example.gateway.integrations.kora.dtos.card.Customer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferPaymentResponseDTO {
    private BigDecimal amount;
    private String currency;
    private Customer customer;
    private Map<String,Object> metaData;

}
