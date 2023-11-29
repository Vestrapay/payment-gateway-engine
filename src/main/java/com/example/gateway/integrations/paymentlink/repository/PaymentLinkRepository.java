package com.example.gateway.integrations.paymentlink.repository;

import com.example.gateway.integrations.paymentlink.entity.PaymentLink;
import com.example.gateway.transactions.enums.Status;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PaymentLinkRepository extends R2dbcRepository<PaymentLink,Long> {
    Mono<PaymentLink> findByMerchantIdAndInvoiceId(String merchantId,String invoiceId);
    Mono<PaymentLink> findByMerchantIdAndPath(String merchantId,String path);
    Mono<PaymentLink> findByMerchantIdAndUuid(String merchantId,String uuid);
    Flux<PaymentLink> findByMerchantId(String merchantId);

    Flux<PaymentLink>findByPathAndStatus(String path, Status status);
    Mono<PaymentLink>findByMerchantIdAndTransactionIdAndStatus(String merchantId,String transactionId, Status status);
}
