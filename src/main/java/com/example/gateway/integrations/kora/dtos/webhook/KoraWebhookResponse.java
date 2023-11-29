package com.example.gateway.integrations.kora.dtos.webhook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KoraWebhookResponse {
    private String event;
    private Data data;
}
