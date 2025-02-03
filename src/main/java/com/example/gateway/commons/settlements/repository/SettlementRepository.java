package com.example.gateway.commons.settlements.repository;

import com.example.gateway.commons.settlements.models.Settlement;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface SettlementRepository extends R2dbcRepository<Settlement,Long> {

    Flux<Settlement> findByMerchantIdAndPrimaryAccount(String merchantId,boolean val);
    Flux<Settlement> findByMerchantIdAndPrimaryAccountAndCurrency(String merchantId,boolean val,String currency);
}
