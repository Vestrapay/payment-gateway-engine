package com.example.gateway.integrations.wemabank.services;

import com.example.gateway.commons.utils.HttpUtil;
import com.example.gateway.integrations.wemabank.accounts.repository.WemaAccountRepository;
import com.example.gateway.integrations.wemabank.dtos.*;
import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
import com.example.gateway.transactions.models.Transaction;
import com.example.gateway.transactions.reporitory.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class WemaBankService implements IWemaBankService {
    private final TransactionRepository transactionRepository;
    private final WemaAccountRepository wemaAccountRepository;
    private final HttpUtil httpUtil;

    @Value("${transaction.query.url}")
    private String transactionQueryUrl;
    @Override
    public Mono<WemaTransactionNotificationResponse> receiveTransactionNotification(WemaTransactionDTO request) {
        Transaction transaction = Transaction.builder().build();
        return transactionRepository.save(transaction)
                .flatMap(transaction1 -> {
                    log.info("transaction notification from wemabank saved successfully");
                    return Mono.just(WemaTransactionNotificationResponse.builder()
                                    .transactionReference(transaction1.getUuid())
                                    .status("OO")
                                    .status("Okay")
                            .build());
                }).onErrorResume(throwable -> {
                    log.error("error saving transaction notification from wemabank error is {}",throwable.getLocalizedMessage());
                    return Mono.just(WemaTransactionNotificationResponse.builder()
                            .status("91")
                            .status("Not-Okay")
                            .build());
                });
    }

    @Override
    public Mono<WemaAccountLookupResponse> accountLookup(AccountLookupRequest request) {
        return wemaAccountRepository.findByAccountNumber(request.getAccountNumber())
                .flatMap(settlement -> {
                    log.info("aaccount gotten. {}",settlement.toString());
                    return Mono.just(WemaAccountLookupResponse.builder()
                                    .accountName(settlement.getAccountName())
                                    .status("00")
                                    .statusDescription("Okay")
                            .build());
                }).switchIfEmpty(Mono.defer(() -> {
                    log.error("account not found for {}", request);
                    return Mono.just(WemaAccountLookupResponse.builder()
                            .status("07")
                            .statusDescription("Invalid Account")
                            .build());
                }));
    }

    @Override
    //for this the transaction amount needs to be provided
    public Mono<WemaTransactionDTO> tranasctionQuery(WemaTransactionQueryRequest request) {
//        return httpUtil.post(transactionQueryUrl,request)
//                .flatMap(clientResponse -> {
//                    if (clientResponse.statusCode().is2xxSuccessful()){
//                        return clientResponse.bodyToMono(TransactionQueryResponse.class)
//                                .flatMap(response -> {
//                                    log.info("response received is {}",response.toString());
//
//                                })
//                    }
//                    else {
//                        log.error("transactions not found");
//
//                    }
//
//                })

        return null;
    }


}
