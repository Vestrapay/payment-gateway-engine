package com.example.gateway.commons.webhook.service;

import com.example.gateway.commons.notificatioin.NotificationService;
import com.example.gateway.commons.transactions.services.TransactionService;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.commons.webhook.interfaces.IWebhookInterface;
import com.example.gateway.integrations.kora.dtos.webhook.KoraWebhookResponse;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.gateway.commons.transactions.enums.Status.SUCCESSFUL;

@Service
@RequiredArgsConstructor
@Slf4j
public class KoraPayWebhook implements IWebhookInterface {
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;


    @Override
    public Mono<Response<Object>> process(Object request, String provider) {
        ObjectMapper mapper = new ObjectMapper();
        KoraWebhookResponse response = mapper.convertValue(request, KoraWebhookResponse.class);
        String reference = response.getData().getReference();
        return transactionRepository.findByTransactionReferenceAndProviderNameEqualsIgnoreCase(reference,provider.toUpperCase())
                .flatMap(transaction -> {
                    if (transaction.getTransactionStatus().equals(SUCCESSFUL)){
                        return Mono.just(Response.builder()
                                .statusCode(HttpStatus.OK.value())
                                .status(HttpStatus.OK)
                                .message(SUCCESSFUL.name())
                                .data(transaction)
                                .build());
                    }
                    if (response.getData().getStatus().equalsIgnoreCase("success")){
                        transaction.setTransactionStatus(SUCCESSFUL);
                    } else if (response.getData().getStatus().equalsIgnoreCase("failed")) {
                        transaction.setTransactionStatus(Status.FAILED);
                    }else if (response.getData().getStatus().equalsIgnoreCase("expired")) {
                        transaction.setTransactionStatus(Status.EXPIRED);
                    }else {
                        transaction.setTransactionStatus(Status.PENDING);
                    }

                    return transactionService.updateTransaction(transaction)
                            .flatMap(transaction1 -> {
                                notificationService.postNotification(transaction1);

                                return Mono.just(Response.builder()
                                        .statusCode(HttpStatus.OK.value())
                                        .status(HttpStatus.OK)
                                        .message(SUCCESSFUL.name())
                                        .data(transaction1)
                                        .build());
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("{} transaction not found with RRN {} for provider {}",response,reference,"KORAPAY");
                    return Mono.just(Response.builder()
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .status(HttpStatus.NOT_FOUND)
                            .errors(List.of("transciton not found"))
                            .message(Status.FAILED.name())
                            .build());
                }));
    }

    @Override
    public String getProvider() {
        return "KORAPAY";
    }
}
