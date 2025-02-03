package com.example.gateway.commons.transactions.services;

import com.example.gateway.commons.utils.PaymentUtils;
import com.example.gateway.commons.webhook.service.CallbackService;
import com.example.gateway.commons.transactions.enums.PaymentTypeEnum;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.models.Transaction;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import com.example.gateway.integrations.paymentlink.repository.PaymentLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CallbackService callbackService;
    private final PaymentLinkRepository paymentLinkRepository;
    public Mono<Transaction> saveTransaction(Transaction transaction, PaymentTypeEnum typeEnum, String merchantId){
        transaction.setUuid(UUID.randomUUID().toString());
        transaction.setTransactionStatus(Status.ONGOING);
        transaction.setPaymentType(typeEnum);
        transaction.setSettlementStatus(Status.PENDING);

        if (typeEnum.equals(PaymentTypeEnum.CARD)){
            transaction.setPan(transaction.getPan().substring(0,6).concat("******").concat(transaction.getPan().substring(transaction.getPan().length()-4)));

        }
        return transactionRepository.findByTransactionReferenceAndMerchantId(transaction.getTransactionReference(),merchantId)
                .flatMap(transaction1 -> {
                    //incase payment has not been done before
                    if (transaction1.getAmount().equals(transaction.getAmount())&& transaction1.getTransactionStatus()!=Status.SUCCESSFUL){
                        return Mono.just(transaction1);
                    }
                    else {
                        log.error("duplicate transaction for merchant");
                        return Mono.error(() -> new Throwable("duplicate transaction. perform a status check with your transaction reference "+transaction.getTransactionReference()));
                    }

                })
                .switchIfEmpty(Mono.defer(() -> transactionRepository.save(transaction)
                        .flatMap(transaction1 -> {
                            log.info("initial transaction saved");
                            return Mono.just(transaction1);
                        })))
                .doFinally(signalType -> {
                    Mono.fromRunnable(() -> {
                        paymentLinkRepository.findByMerchantIdAndTransactionId(transaction.getMerchantId(),transaction.getTransactionReference())
                                .doOnNext(paymentLinks -> {
                                    paymentLinks.setStatus(Status.PROCESSING);
                                    paymentLinkRepository.updatePaymentLinkByMerchantId(paymentLinks.getId(),Status.PROCESSED.name()).subscribe();
                                }).subscribe();
                    }).subscribe();
                });

    }

    public Mono<Transaction> updateTransaction(Transaction transaction){
        return transactionRepository.save(transaction)
                .flatMap(transaction1 -> {

                    callbackService.sendNotification(transaction1);
                    return Mono.just(transaction1);
                });
    }
}
