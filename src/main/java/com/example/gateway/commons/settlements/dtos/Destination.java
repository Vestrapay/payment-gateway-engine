package com.example.gateway.commons.settlements.dtos;

import com.example.gateway.integrations.kora.dtos.card.Customer;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Destination {
    private String type;
    private String amount;
    private String currency;
    private String narration;
    @JsonProperty("bank_account")
    private BankAccount bankAccount;
    private Customer customer;

}
