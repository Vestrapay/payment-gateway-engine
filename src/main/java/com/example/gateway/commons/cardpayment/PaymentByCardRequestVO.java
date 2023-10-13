package com.example.gateway.commons.cardpayment;

import com.example.gateway.commons.Currency;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;



@Data
@Builder
@EqualsAndHashCode
public class PaymentByCardRequestVO {


    @JsonIgnore
    private String deviceInformation;

    @JsonIgnore
    private String callBackUrl;

    @NotBlank(message = "reference is required")
    private String reference;

    @Valid
    private CardDetails cardDetails;

    @NotBlank(message = "Amount is required")
    private String amount;
    
    private Currency currency;

    @NotBlank(message = "customerId is required")
    private String customerId;

    private String merchantId;

}
