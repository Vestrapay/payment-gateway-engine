package com.example.gateway.integrations.budpay.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransferData {
    private String reference;
    private String currency;
    private BigDecimal amount;
    private BigDecimal fee;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String narration;
    private String domain;
    private String status;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

}
