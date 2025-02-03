package com.example.gateway.api.transfer.services;

import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.keys.repository.KeysRepository;
import com.example.gateway.api.transfer.dtos.TransferPaymentRequestDTO;
import com.example.gateway.api.transfer.interfaces.ITransferService;
import com.example.gateway.commons.merchants.service.ValidateUserKycService;
import com.example.gateway.commons.repository.RoutingRuleRepository;
import com.example.gateway.commons.transactions.enums.PaymentTypeEnum;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
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
public class TransferPaymentService {
    public static final String KEYS_NOT_FOUND_FOR_MERCHANT = "keys not found for merchant";
    private final TransactionRepository transactionRepository;
    private final KeysRepository keysRepository;
    private static final String FAILED  = "Failed";
    private static final String SUCCESSFUL  = "Successful";
    @Value("${server.environment}")
    String environment;
    private final ValidateUserKycService validateUserKycService;
    private final List<ITransferService> transferServiceFactory;
    private final RoutingRuleRepository routingRuleRepository;

    public Mono<Response<Object>> payWithTransfer(String key, String merchantId, TransferPaymentRequestDTO request, String customerId) {
        return validateUserKycService.isKycComplete(merchantId)
                .flatMap(aBoolean -> {
                    if (aBoolean){
                        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,key,environment)
                                .flatMap(keys -> {
                                    return routingRuleRepository.findByPaymentMethodAndCurrency(PaymentTypeEnum.TRANSFER.name(), request.getCurrency())
                                            .flatMap(routingRule -> {
                                                return transferServiceFactory.stream().filter(iTransferService -> iTransferService.getName().equalsIgnoreCase(routingRule.getProvider().toUpperCase()))
                                                        .findFirst()
                                                        .orElseThrow(() -> new RuntimeException("transfer implementation not found"))
                                                        .payWithTransfer(request,merchantId,customerId);

                                            })
                                            .switchIfEmpty(Mono.defer(() -> {
                                                return Mono.just(Response.builder()
                                                        .errors(List.of("no implementation for provider"))
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .message("Failed")
                                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .build());
                                            }));
                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.error(KEYS_NOT_FOUND_FOR_MERCHANT);
                                    return Mono.just(Response.builder()
                                            .status(HttpStatus.BAD_REQUEST)
                                            .statusCode(HttpStatus.BAD_REQUEST.value())
                                            .message(FAILED)
                                            .errors(List.of(environment+KEYS_NOT_FOUND_FOR_MERCHANT))
                                            .build());
                                }))
                                .doOnError(CustomException.class, customException -> {
                                    throw new CustomException(Response.builder()
                                            .message(FAILED)
                                            .statusCode(customException.getHttpStatus().value())
                                            .status(customException.getHttpStatus())
                                            .data(customException.getResponse().getData())
                                            .errors(customException.getResponse().getErrors())
                                            .build(), customException.getHttpStatus());
                                });
                    }
                    log.error("merchant KYC incomplete for {}",merchantId);
                    return Mono.just(Response.builder()
                            .status(HttpStatus.UNAUTHORIZED)
                            .errors(List.of("Merchant KYC not complete"))
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data("Merchant KYC not complete")
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

}
