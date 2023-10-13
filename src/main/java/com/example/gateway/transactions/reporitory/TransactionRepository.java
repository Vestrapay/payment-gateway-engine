package com.example.gateway.transactions.reporitory;

import com.example.gateway.transactions.models.Transaction;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface TransactionRepository extends R2dbcRepository<Transaction,Long> {
    Mono<Transaction> findByTransactionReferenceAndMerchantIdAndUuid(String TransactionRef,String merchantId, String UUID );

}
