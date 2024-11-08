package com.example.gateway.commons.webhook.interfaces;

import com.example.gateway.commons.utils.Response;
import reactor.core.publisher.Mono;

public interface IWebhookInterface {
    Mono<Response<Object>> process(Object request, String provider);
    String getProvider();
}