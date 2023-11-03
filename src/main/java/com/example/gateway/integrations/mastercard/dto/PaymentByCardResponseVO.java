package com.example.gateway.integrations.mastercard.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentByCardResponseVO extends BaseResponse {

    private String paymentId;
    private String amount;
    private String transactionReference;
    private String transactionId;
    private String plainTextSupportMessage;
    private String acsUrl;
    private String termUrl;
    private String eciFlag;
    private String md;
    private String paReq;
    private String token;
    private String jwt;
    private String method;
    @JsonIgnore
    private String clientId;
    private String currency;
}
