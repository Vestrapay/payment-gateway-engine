package com.example.gateway.commons.repository;

import com.example.gateway.commons.models.RoutingRule;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface RoutingRuleRepository extends R2dbcRepository<RoutingRule,Long> {
    Mono<RoutingRule>findByPaymentMethod(String paymentMethod);
}
