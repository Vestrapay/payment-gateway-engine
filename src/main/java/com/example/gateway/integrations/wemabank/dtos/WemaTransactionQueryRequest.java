package com.example.gateway.integrations.wemabank.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WemaTransactionQueryRequest {
    @JsonProperty("sessionid")
    private String sessionId;
    @JsonProperty("craccount")
    private String crAccount;
    private String amount;
    @JsonProperty("txndate")
    private String transactionDate;
}
