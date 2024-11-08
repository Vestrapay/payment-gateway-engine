package com.example.gateway.integrations.kora.dtos.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Data {
    private BigDecimal amount;
    @JsonProperty("amount_charged")
    private double amountCharged;
    @JsonProperty("auth_model")
    private String authModel;
    private String currency;
    private BigDecimal fee;
    private BigDecimal vat;
    @JsonProperty("response_message")
    private String responseMessage;
    @JsonProperty("payment_reference")
    private String paymentReference;
    private String status;
    @JsonProperty("transaction_reference")
    private String transactionReference;
    public Authorization authorization;
    public MetaData metadata;
    public Card card;}
