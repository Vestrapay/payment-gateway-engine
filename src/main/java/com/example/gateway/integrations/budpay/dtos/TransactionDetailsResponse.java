package com.example.gateway.integrations.budpay.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDetailsResponse {
    private boolean status;
    private String message;
    private DataDetails data;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataDetails {
        private String status;
        private int id;
        private String transactionReference;
        private double amount;
        private double chargedAmount;
        private String currency;
        private double transactionFee;
        private double merchantFee;
        private String gatewayResponseCode;
        private String gatewayResponseMessage;
        private String domain;
        private String channel;
        private String paymentDescriptor;
        private CardDetails card;
        private CustomerDetails customer;
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CardDetails {
        private String first6Digits;
        private String last4Digits;
        private String issuer;
        private String country;
        private String type;
        private String token;
        private String expiry;
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerDetails {
        private int id;
        private String customerCode;
        private String email;
        private String name;
    }
}
