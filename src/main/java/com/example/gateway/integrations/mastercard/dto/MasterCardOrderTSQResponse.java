package com.example.gateway.integrations.mastercard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasterCardOrderTSQResponse {

    private String result;
    private MasterCardHTTPErrorResponse error;
    private MasterCardTransactionResponse response;
    private String status;
    private BigDecimal totalAuthorizedAmount;
    private BigDecimal totalCapturedAmount;
    private BigDecimal totalRefundedAmount;
    private BigDecimal amount;
    private String creationTime;
    private String lastUpdatedTime;
}
