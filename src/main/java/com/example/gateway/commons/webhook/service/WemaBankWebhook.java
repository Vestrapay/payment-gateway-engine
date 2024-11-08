package com.example.gateway.commons.webhook.service;

import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.wemabank.dtos.WemaTransactionDTO;
import com.example.gateway.integrations.wemabank.services.WemaBankService;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.webhook.interfaces.IWebhookInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.gateway.commons.transactions.enums.Status.SUCCESSFUL;

@Service
@Slf4j
@RequiredArgsConstructor
public class WemaBankWebhook implements IWebhookInterface {
    private final WemaBankService wemaBankService;

    @Override
    public Mono<Response<Object>> process(Object request, String provider) {
        ObjectMapper mapper = new ObjectMapper();
        WemaTransactionDTO wemaTransactionDTO = mapper.convertValue(request, WemaTransactionDTO.class);
        return wemaBankService.receiveTransactionNotification(wemaTransactionDTO)
                .flatMap(wemaTransactionNotificationResponse -> Mono.just(Response.builder()
                        .data(wemaTransactionNotificationResponse)
                        .statusCode(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .message(SUCCESSFUL.name())
                        .build()))
                .doOnError(throwable -> {
                    log.error("error receiving wema transaction notification. error is {}",throwable.getMessage());
                    throw new CustomException(Response.builder()
                            .message(Status.FAILED.name())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .errors(List.of(throwable.getMessage()))
                            .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                });    }

    @Override
    public String getProvider() {
        return "WEMA";
    }
}
