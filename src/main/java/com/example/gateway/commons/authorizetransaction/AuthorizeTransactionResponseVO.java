package com.example.gateway.commons.authorizetransaction;

import com.example.gateway.commons.BaseResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Data
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizeTransactionResponseVO extends BaseResponse {

    private String paymentId;

    private String transactionRef;

    private String providerReference;

    private String amount;

}
