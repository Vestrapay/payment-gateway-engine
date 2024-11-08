package com.example.gateway.integrations.kora.dtos.transfer;

import com.example.gateway.integrations.kora.dtos.card.Customer;
import com.example.gateway.commons.transactions.models.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@lombok.Data
@Builder
public class Data {
    private String currency;
    private BigDecimal amount;
    @JsonProperty("amount_expected")
    private BigDecimal amountExpected;
    private BigDecimal fee;
    private BigDecimal vat;
    private String reference;
    @JsonProperty("payment_reference")
    private String paymentReference;
    private String status;
    private String narration;
    @JsonProperty("merchant_bears_cost")
    private boolean merchantBearsCost;
    @JsonProperty("bank_account")
    private BankAccount bankAccount;
    private Customer customer;
    private Transaction transaction;
}
