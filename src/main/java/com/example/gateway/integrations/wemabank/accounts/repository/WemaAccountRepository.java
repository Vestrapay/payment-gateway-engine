package com.example.gateway.integrations.wemabank.accounts.repository;

import com.example.gateway.integrations.wemabank.accounts.models.WemaAccounts;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface WemaAccountRepository extends R2dbcRepository<WemaAccounts,Long> {
    Mono<WemaAccounts>findByAccountNumber(String accountNumber);
    Mono<WemaAccounts>findByMerchantId(String merchantId);
}
