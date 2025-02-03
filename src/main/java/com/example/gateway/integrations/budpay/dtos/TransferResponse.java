package com.example.gateway.integrations.budpay.dtos;

import lombok.Data;

@Data
public class TransferResponse {
    private boolean success;
    private String message;
    private TransferData data;
}
