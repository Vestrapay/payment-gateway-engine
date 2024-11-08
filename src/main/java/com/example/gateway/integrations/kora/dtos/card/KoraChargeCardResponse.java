package com.example.gateway.integrations.kora.dtos.card;

import com.example.gateway.commons.transactions.models.Transaction;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KoraChargeCardResponse {
    private boolean status;
    private String message;
    private Data data;
    private Transaction transaction;

}
