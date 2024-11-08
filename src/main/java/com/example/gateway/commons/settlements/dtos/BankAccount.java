package com.example.gateway.commons.settlements.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BankAccount {
    private String bank;
    private String account;

}
