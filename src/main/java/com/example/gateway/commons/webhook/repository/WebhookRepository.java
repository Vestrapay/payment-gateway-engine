package com.example.gateway.commons.webhook.repository;

import com.example.gateway.commons.webhook.model.Webhook;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface WebhookRepository extends R2dbcRepository<Webhook,Long> {
    Mono<Webhook>findByMerchantId(String merchantId);
}
