package com.example.gateway.commons.transactions.reporitory;

import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.models.Transaction;
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
    Mono<Transaction> findByMerchantIdAndTransactionReferenceOrProviderReferenceAndUserId(String merchantId, String reference, String providerRef, String userId);

    Mono<Transaction> findByTransactionReferenceAndProviderNameEqualsIgnoreCase(String TransactionRef,String providerName);

//    @Query("select * from vestrapay_transactions where transaction_status = :status and settlement_status=:settlementStatus and created_at<= :createdAt limit 100")
//    Flux<Transaction> fetchTransactionsForSettlement(@Param("status") Status status,
//                                                     @Param("settlementStatus") Status settlementStatus,
//                                                     @Param("createdAt")LocalDateTime createdAt);

    @Query("SELECT * FROM vestrapay_transactions WHERE transaction_status = :status and settlement_status=:settlementStatus and created_at >= NOW() - INTERVAL '1 day' AND created_at <= NOW() LIMIT 100;")
    Flux<Transaction> fetchTransactionsForSettlement(@Param("status") Status status,
                                                     @Param("settlementStatus") Status settlementStatus,@Param("created_at")LocalDateTime  createdAt);

    Flux<Transaction> findByTransactionStatusAndSettlementStatusAndCreatedAtBetween(Status status,Status settlementStatus, LocalDateTime start, LocalDateTime end);

    @Query("select * from vestrapay_transactions where created_at >= CURRENT_DATE - INTERVAL '5 minutes' and (transaction_status='ONGOING' or transaction_status='PROCESSING') and scheme ='TRANSFER' limit 100 ")
    Flux<Transaction> getFailedTransactions();

}
