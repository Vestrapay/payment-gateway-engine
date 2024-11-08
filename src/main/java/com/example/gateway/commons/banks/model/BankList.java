package com.example.gateway.commons.banks.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BankList {
    private boolean status;
    private String message;
    private List<Bank> data;
}
