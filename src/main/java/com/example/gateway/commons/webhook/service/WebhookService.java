package com.example.gateway.commons.webhook.service;

import com.example.gateway.commons.utils.Response;
import com.example.gateway.commons.webhook.interfaces.IWebhookInterface;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
@Slf4j
@Service
public class WebhookService {
    private final WebhookFactory webhookFactory;
    public Mono<Response<Object>> process(@Valid @NotNull Object request, String provider) {
        IWebhookInterface webhookInterface = webhookFactory.getImplementation(provider);
        return webhookInterface.process(request,provider);

    }

}
