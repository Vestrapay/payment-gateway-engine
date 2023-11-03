package com.example.gateway.integrations.kora.dtos.card;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KoraChargeCardResponse {
    private boolean status;
    private String message;
    private Data data;

}
