package com.example.gateway.integrations.mastercard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class MasterCardMakePaymentRequest {

    private String apiOperation;
    private MasterCardOrder order;
    private MasterCardTransaction transaction;
    private MasterCardSourceOfFunds sourceOfFunds;
    private MasterCardSession session;


}
