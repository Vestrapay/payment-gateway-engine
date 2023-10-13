package com.example.gateway.integrations.wemabank.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WemaTransactionDTO {
    @JsonProperty("originatoraccountnumber")
    private String originatorAccountNumber;
    @JsonProperty("amount")
    private String amount;
    @JsonProperty("originatorname")
    private String originatorName;
    @JsonProperty("narration")
    private String narration;
    @JsonProperty("craccountname")
    private String crAccountName;
    @JsonProperty("paymentreference")
    private String paymentReference;
    @JsonProperty("bankname")
    private String bankName;
    @JsonProperty("sessionid")
    private String sessionId;
    @JsonProperty("craccount")
    private String crAccount;
    @JsonProperty("bankcode")
    private String bankCode;

}
