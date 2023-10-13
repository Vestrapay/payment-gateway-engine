package com.example.gateway.commons.authorizetransaction;

import com.example.gateway.commons.cardpayment.CardDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorizeTransactionRequestVO {


    @Valid
    private CardDetails cardDetails;

    @NotBlank(message = "paymentId  is required")
    private String paymentId;

    private String otp;

    private String transactionId;
    private String eciFlag;
}
