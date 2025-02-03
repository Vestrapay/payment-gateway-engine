package com.example.gateway.commons.charge.repository;


import com.example.gateway.commons.charge.enums.ChargeCategory;
import com.example.gateway.commons.charge.enums.PaymentMethod;
import com.example.gateway.commons.charge.model.Charge;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChargeRepository extends R2dbcRepository<Charge,Long> {
    Mono<Charge> findByMerchantIdAndPaymentMethodAndCategoryAndCurrency(String merchantId, PaymentMethod paymentMethod, ChargeCategory category,String currency);
    Flux<Charge> findAllByMerchantId(String merchantId);
    Mono<Charge> findByUuid(String chargeId);
}
