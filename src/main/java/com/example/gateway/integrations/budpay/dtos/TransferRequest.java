package com.example.gateway.integrations.budpay.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TransferRequest {
    private String currency;
    private BigDecimal amount;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String narration;
    private List<MetaData> metaData;
}
