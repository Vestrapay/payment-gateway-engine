package com.example.gateway.commons.webhook.service;

import com.example.gateway.commons.utils.Response;
import com.example.gateway.commons.webhook.interfaces.IWebhookInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@RequiredArgsConstructor
@Service
public class MasterCardWebhook implements IWebhookInterface {
    @Override
    public Mono<Response<Object>> process(Object request, String provider) {
        // TODO: 03/11/2023 write implementation when mastercard is up
        return Mono.empty();    }

    @Override
    public String getProvider() {
        return "MASTERCARD";
    }
}
