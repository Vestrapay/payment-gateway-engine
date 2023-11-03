package com.example.gateway.integrations.mastercard.service;

import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.mastercard.dto.*;
import com.example.gateway.integrations.mastercard.utils.MpgsHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MpgsService {
    private final MpgsHttpClient client;

    private static final int MASTERCARD_AUTH_LIMIT = 25;

    public Mono<MasterCardCreateSessionResponse> createSession(String merchantId){
        log.info("performing create session on mastercard api");
        return client.post("/merchant/" + merchantId + "/session", MasterCardCreateSessionRequest.builder()
                        .session(MasterCardSession.builder()
                                .authenticationLimit(MASTERCARD_AUTH_LIMIT).build())
                        .build())
                        .flatMap(clientResponse -> {
                            log.info("ClientResponse code:: {}",clientResponse.statusCode().value());

                            if (clientResponse.statusCode().is2xxSuccessful()) {
                                return clientResponse.bodyToMono(MasterCardCreateSessionResponse.class)
                                        .flatMap(response -> {
                                            log.info("The api res is {}", response);
                                            return Mono.just(response);
                                        });
                            }
                            else {
                                log.error("unsuccessful response from mastercard create session");
                                return clientResponse.bodyToMono(Map.class)
                                        .flatMap(map -> {
                                            log.error(map.toString());
                                            return Mono.error(new CustomException(Response.builder()
                                                    .errors(List.of("unsuccessful create session response from MasterCard"))
                                                    .message("FAILED")
                                                    .data(map)
                                                    .status(HttpStatus.valueOf(clientResponse.statusCode().value()))
                                                    .statusCode(HttpStatus.valueOf(clientResponse.statusCode().value()).value())
                                                    .build(), HttpStatus.valueOf(clientResponse.statusCode().value())));
                                        });

                            }

                        });
    }

    public Mono<?> updateSession(MasterCardUpdateSessionRequest request, String sessionId, String merchantId){

        log.info("performing update session on mastercard api " + request);
        return client.put("/merchant/" + merchantId + "/session/" + sessionId,
                request)
                .flatMap(clientResponse -> {
                    log.info("ClientResponse code:: {}",clientResponse.statusCode().value());

                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(MasterCardUpdateSessionResponse.class)
                                .flatMap(response -> {
                                    log.info("The api res is {}", response);
                                    return Mono.just(response);
                                });
                    }
                    else {
                        return clientResponse.bodyToMono(Map.class)
                                .flatMap(response -> {
                                    log.info("The api res is {}", response);
                                    return Mono.error(new CustomException("unsuccessful update session from MasterCard"));                                });
                    }
                });

    }

    public Mono<MasterCardMakePaymentResponse> makePayment(MasterCardMakePaymentRequest request, String merchantId){
        log.info("performing make payment on mastercard api " + request);

        return client.put("/merchant/" + merchantId +
                        "/order/" + request.getOrder().getReference() + "/transaction/" + request.getTransaction().getReference(), request)
                        .flatMap(clientResponse -> {
                            if (clientResponse.statusCode().is2xxSuccessful()) {
                                log.info("successful make payment response");
                                return clientResponse.bodyToMono(MasterCardMakePaymentResponse.class)
                                        .flatMap(response -> {
                                            log.info("The api res is {}", response);
                                            return Mono.just(response);
                                        });
                            }
                            else {
                                return clientResponse.bodyToMono(Map.class)
                                        .flatMap(map -> Mono.error(new CustomException(Response.builder()
                                                .statusCode(clientResponse.statusCode().value())
                                                .status(HttpStatus.valueOf(clientResponse.statusCode().value()))
                                                .data(map)
                                                .build(), HttpStatus.valueOf(clientResponse.statusCode().value()))));
                            }


                        });

    }

    public Mono<MasterCardOrderTSQResponse> transactionStatus(final MasterCardTSQRequest request, String merchantId) {
        log.info("performing tsq on mastercard api " + request);
        return client.get("/merchant/" + merchantId +
                "/order/" + request.getOrder())
                .flatMap(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        log.info("successful TSQ response");
                        return clientResponse.bodyToMono(MasterCardOrderTSQResponse.class)
                                .flatMap(response -> {
                                    log.info("The api res is {}", response);
                                    return Mono.just(response);
                                });
                    }
                    return Mono.error(new CustomException("unsuccessful TSQ response MasterCard"));
                });
    }

}
