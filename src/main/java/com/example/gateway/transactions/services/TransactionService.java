package com.example.gateway.transactions.services;

import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.utils.PaymentUtils;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.transactions.enums.PaymentTypeEnum;
import com.example.gateway.transactions.enums.Status;
import com.example.gateway.transactions.models.Transaction;
import com.example.gateway.transactions.reporitory.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    public Mono<?> saveTransaction(Transaction transaction, PaymentTypeEnum typeEnum, String merchantId){
        transaction.setUuid(UUID.randomUUID().toString());
        transaction.setTransactionStatus(Status.ONGOING);
        transaction.setPaymentType(typeEnum);
        transaction.setSettlementStatus(Status.PENDING);

        if (typeEnum.equals(PaymentTypeEnum.CARD)){
            transaction.setCardScheme(PaymentUtils.detectCardScheme(transaction.getPan()));
            transaction.setPan(transaction.getPan().substring(0,6).concat("******").concat(transaction.getPan().substring(transaction.getPan().length()-4)));

        }
        return transactionRepository.findByTransactionReferenceAndMerchantId(transaction.getTransactionReference(),merchantId)
                .flatMap(transaction1 -> {
                    log.error("duplicate transaction for merchant");
                    return Mono.error(new CustomException(Response.builder()
                            .message("FAILED")
                            .errors(List.of("duplicate transaction"))
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .build(), HttpStatus.BAD_REQUEST));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    return transactionRepository.save(transaction)
                            .flatMap(transaction1 -> {
                                log.info("initial transaction saved");
                                return Mono.just(transaction1);
                            });
                }));

    }
}
