package com.example.gateway.api.card.services;

import com.example.gateway.api.card.dtos.CardPaymentRequestDTO;
import com.example.gateway.api.card.interfaces.ICardService;
import com.example.gateway.commons.keys.repository.KeysRepository;
import com.example.gateway.commons.merchants.service.ValidateUserKycService;
import com.example.gateway.commons.repository.RoutingRuleRepository;
import com.example.gateway.commons.transactions.enums.PaymentTypeEnum;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestAVS;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestOTP;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestPhone;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestPin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardPaymentService  {
    public static final String KEYS_NOT_FOUND_FOR_MERCHANT = "keys not found for merchant";
    private final TransactionRepository transactionRepository;
    private final KeysRepository keysRepository;
    private static final String FAILED  = "Failed";
    private static final String SUCCESSFUL  = "Successful";
    @Value("${server.environment}")
    String environment;
    private final ValidateUserKycService validateUserKycService;
    private final List<ICardService> cardServices;
    private final RoutingRuleRepository routingRuleRepository;


    public Mono<Response<?>> payWithCard(String key, String merchantId, CardPaymentRequestDTO request, String customerId) {
        return validateUserKycService.isKycComplete(merchantId)
                .flatMap(aBoolean -> {
                    if (aBoolean){
                        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,key,environment.toUpperCase())
                                .flatMap(keys -> {
                                    log.info("incoming payment request {}",request);
                                    return routingRuleRepository.findByPaymentMethodAndCurrency(PaymentTypeEnum.CARD.name(), request.getCurrency())
                                            .flatMap(routingRule -> {
                                                return cardServices.stream().filter(iCardService -> iCardService.getName().equalsIgnoreCase(routingRule.getProvider().toUpperCase()))
                                                        .findFirst().orElseThrow(() -> new RuntimeException("no implementation found"))
                                                        .payWithCard(request,merchantId,customerId);
                                            }).switchIfEmpty(Mono.defer(() -> {
                                                log.error("Routing rule not configured for gateway check configurations");
                                                return Mono.just(Response.builder()
                                                        .errors(List.of("routing rule not configured for gateway."))
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .message("Failed")
                                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .build());
                                            }));                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.error("keys not found for user in environment {}",environment);
                                    return Mono.just(Response.builder()
                                            .status(HttpStatus.BAD_REQUEST)
                                            .errors(List.of(environment+KEYS_NOT_FOUND_FOR_MERCHANT))
                                            .statusCode(HttpStatus.BAD_REQUEST.value())
                                            .message(FAILED)
                                            .errors(List.of(KEYS_NOT_FOUND_FOR_MERCHANT))
                                            .build());
                                }));
                    }
                    log.error("merchant Kyc not complete for merchant id {}",merchantId);
                    return Mono.just(Response.builder()
                            .status(HttpStatus.UNAUTHORIZED)
                            .errors(List.of("Merchant KYC not complete"))
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data("Merchant KYC not complete")
                            .message(FAILED)
                            .build());
                }).onErrorResume(throwable -> {
                    return Mono.just(Response.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .errors(List.of(throwable.getMessage()))
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .data(throwable.getMessage())
                            .message(FAILED)
                            .build());
                });


    }

    public Mono<Response<Object>> doTSQ(String secret, String merchantId, String reference, String customerId) {
        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,secret,environment)
                .flatMap(keys -> {
                    return transactionRepository.findByMerchantIdAndTransactionReferenceOrProviderReferenceAndUserId(merchantId,reference,reference,customerId)
                            .flatMap(transaction -> Mono.just(Response.builder()
                                    .data(transaction)
                                    .message(SUCCESSFUL)
                                    .status(HttpStatus.OK)
                                    .statusCode(HttpStatus.OK.value())
                                    .build()))
                            .switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                                    .message(FAILED)
                                    .statusCode(HttpStatus.NOT_FOUND.value())
                                    .status(HttpStatus.NOT_FOUND)
                                    .errors(List.of("Transaction not found"))
                                    .build())));

                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("keys not found for user");
                    return Mono.just(Response.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(FAILED)
                            .errors(List.of(environment+KEYS_NOT_FOUND_FOR_MERCHANT))
                            .build());
                })).cache(Duration.ofMinutes(2));
    }

    public Mono<Response<Object>> authorizeCard(AuthorizeCardRequestPin request, String merchantId, String userId) {
        return routingRuleRepository.findByPaymentMethodAndCurrency(PaymentTypeEnum.CARD.name(), request.getCurrency())
                .flatMap(routingRule -> {
                    return cardServices.stream().filter(iCardService -> iCardService.getName().equalsIgnoreCase(routingRule.getProvider().toUpperCase()))
                            .findFirst().orElseThrow(() -> new RuntimeException("no implementation found"))
                            .authorizeCard(request,merchantId,userId);
                }).switchIfEmpty(Mono.defer(() -> {
                    log.error("Routing rule not configured for gateway check configurations");
                    return Mono.just(Response.builder()
                            .errors(List.of("routing rule not configured for gateway."))
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Failed")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
                }));
    }

    public Mono<Response<Object>> authorizeCardOtp(AuthorizeCardRequestOTP request, String merchantId, String userId) {
        return routingRuleRepository.findByPaymentMethodAndCurrency(PaymentTypeEnum.CARD.name(),request.getCurrency())
                .flatMap(routingRule -> {
                    return cardServices.stream().filter(iCardService -> iCardService.getName().equalsIgnoreCase(routingRule.getProvider().toUpperCase()))
                            .findFirst().orElseThrow(() -> new RuntimeException("no implementation found"))
                            .authorizeCardOtp(request,merchantId,userId);
                }).switchIfEmpty(Mono.defer(() -> {
                    log.error("Routing rule not configured for gateway check configurations");
                    return Mono.just(Response.builder()
                            .errors(List.of("routing rule not configured for gateway."))
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Failed")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
                }));
    }

    public Mono<Response<Object>> authorizeCardAvs(AuthorizeCardRequestAVS request, String merchantId, String userId) {
        return routingRuleRepository.findByPaymentMethodAndCurrency(PaymentTypeEnum.CARD.name(), request.getCurrency())
                .flatMap(routingRule -> {
                    return cardServices.stream().filter(iCardService -> iCardService.getName().equalsIgnoreCase(routingRule.getProvider().toUpperCase()))
                            .findFirst().orElseThrow(() -> new RuntimeException("no implementation found"))
                            .authorizeCardAvs(request,merchantId,userId);
                }).switchIfEmpty(Mono.defer(() -> {
                    log.error("Routing rule not configured for gateway check configurations");
                    return Mono.just(Response.builder()
                            .errors(List.of("routing rule not configured for gateway."))
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Failed")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
                }));
    }

    public Mono<Response<Object>> authorizeCardPhone(AuthorizeCardRequestPhone request, String merchantId, String userId) {
        return routingRuleRepository.findByPaymentMethodAndCurrency(PaymentTypeEnum.CARD.name(), request.getCurrency())
                .flatMap(routingRule -> {
                    return cardServices.stream().filter(iCardService -> iCardService.getName().equalsIgnoreCase(routingRule.getProvider().toUpperCase()))
                            .findFirst().orElseThrow(() -> new RuntimeException("no implementation found"))
                            .authorizeCardPhone(request,merchantId,userId);
                }).switchIfEmpty(Mono.defer(() -> {
                    log.error("Routing rule not configured for gateway check configurations");
                    return Mono.just(Response.builder()
                            .errors(List.of("routing rule not configured for gateway."))
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Failed")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
                }));
    }

}
