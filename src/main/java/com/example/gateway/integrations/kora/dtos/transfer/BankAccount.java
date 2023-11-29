package com.example.gateway.integrations.kora.dtos.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
public class BankAccount {
    @JsonProperty("account_name")
    private String accountName;
    @JsonProperty("account_number")
    private String accountNumber;
    @JsonProperty("bank_name")
    private String bankName;
    @JsonProperty("bank_code")
    private String bankCode;
    @JsonProperty("expiry_date_in_utc")
    private Date expiryDateInUtc;
}
