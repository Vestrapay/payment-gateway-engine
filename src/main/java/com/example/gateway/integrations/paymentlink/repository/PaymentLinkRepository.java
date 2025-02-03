package com.example.gateway.integrations.paymentlink.repository;

import com.example.gateway.integrations.paymentlink.entity.PaymentLink;
import com.example.gateway.commons.transactions.enums.Status;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PaymentLinkRepository extends R2dbcRepository<PaymentLink,Long> {
    Mono<PaymentLink> findByMerchantIdAndInvoiceId(String merchantId,String invoiceId);
    Mono<PaymentLink> findByMerchantIdAndPath(String merchantId,String path);
    Mono<PaymentLink> findByMerchantIdAndPathAndStatus(String merchantId,String path,Status status);
    Mono<PaymentLink> findByMerchantIdAndUuid(String merchantId,String uuid);
    Flux<PaymentLink> findByMerchantIdOrderByDateCreatedDesc(String merchantId);

    Mono<PaymentLink>findByPathAndStatusAndTransactionId(String path, Status status,String reference);
    Mono<PaymentLink>findByMerchantIdAndTransactionId(String merchantId,String tranId);
    Mono<PaymentLink>findByMerchantIdAndTransactionIdAndStatus(String merchantId,String transactionId, Status status);
    @Modifying
    @Query("update payment_link set status = :status where id = :id")
    Mono<Integer> updatePaymentLinkByMerchantId(@Param("id")Long id,@Param("status")String status);



}
