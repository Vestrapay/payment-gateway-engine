package com.example.gateway.transactions.reporitory;

import com.example.gateway.transactions.enums.Status;
import com.example.gateway.transactions.models.Transaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionRepository extends R2dbcRepository<Transaction,Long> {
    Mono<Transaction> findByTransactionReferenceAndMerchantIdAndUuid(String TransactionRef,String merchantId, String UUID );
    Mono<Transaction> findByTransactionReferenceAndMerchantId(String TransactionRef,String merchantId);
    Mono<Transaction> findByTransactionReferenceAndAmount(String TransactionRef, BigDecimal amount);
    Mono<Transaction> findByMerchantIdAndTransactionReferenceOrProviderReference(String merchantId,String reference,String providerRef);

    Mono<Transaction> findByTransactionReferenceAndProviderName(String TransactionRef,String providerName);

    @Query("select * from vestrapay_transactions where transaction_status = :status and settlement_status=:settlementStatus and created_at<= :createdAt limit 100")
    Flux<Transaction> fetchTransactionsForSettlement(@Param("status") Status status,
                                                     @Param("settlementStatus") Status settlementStatus,
                                                     @Param("createdAt")LocalDateTime createdAt);

}
