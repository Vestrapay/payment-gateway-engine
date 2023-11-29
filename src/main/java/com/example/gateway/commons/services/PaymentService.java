package com.example.gateway.commons.services;

import com.example.gateway.commons.dto.card.CardPaymentRequestDTO;
import com.example.gateway.commons.dto.paymentlink.PaymentLinkRequestDTO;
import com.example.gateway.commons.dto.transfer.TransferPaymentRequestDTO;
import com.example.gateway.commons.dto.ussd.USSDPaymentRequestDTO;
import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.interfaces.IPaymentService;
import com.example.gateway.commons.keys.repository.KeysRepository;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.webhook.KoraWebhookResponse;
import com.example.gateway.integrations.wemabank.dtos.WemaTransactionDTO;
import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
import com.example.gateway.transactions.enums.PaymentTypeEnum;
import com.example.gateway.transactions.enums.Status;
import com.example.gateway.transactions.reporitory.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class PaymentService implements IPaymentService {
    public static final String KEYS_NOT_FOUND_FOR_MERCHANT = "keys not found for merchant";
    private final TransactionRepository transactionRepository;
    private final KeysRepository keysRepository;
    private static final String FAILED  = "Failed";
    private static final String SUCCESSFUL  = "Successful";
    private final PaymentServiceRouter paymentServiceRouter;
    @Value("${server.environment}")
    String environment;
    private final IWemaBankService wemaBankService;
    @Override
    public Mono<Response<?>> payWithCard(String key, String merchantId, CardPaymentRequestDTO request) {

        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,key,environment.toUpperCase())
                .flatMap(keys -> {
                    return paymentServiceRouter.routePayment(PaymentTypeEnum.CARD,request,keys.getUserId());
                })
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

    @Override
    public Mono<Response<?>> payWithTransfer(String key, String merchantId, TransferPaymentRequestDTO request) {

        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,key,environment)
                .flatMap(keys -> {
                    return paymentServiceRouter.routePayment(PaymentTypeEnum.TRANSFER,request, keys.getUserId());
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

    @Override
    public Mono<Response<Object>> payWithUSSD(String key, String merchantId, USSDPaymentRequestDTO request) {
        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,key,environment)
                .flatMap(keys -> {
                    return Mono.just(Response.builder()
                            .status(HttpStatus.NO_CONTENT)
                            .statusCode(HttpStatus.NO_CONTENT.value())
                            .message(FAILED)
                            .errors(List.of(environment+KEYS_NOT_FOUND_FOR_MERCHANT))
                            .build());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error(KEYS_NOT_FOUND_FOR_MERCHANT);
                    return Mono.just(Response.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(FAILED)
                            .errors(List.of(environment+KEYS_NOT_FOUND_FOR_MERCHANT))
                            .build());
                }));
    }

    @Override
    public Mono<Response<?>> payWithPaymentLink(String key, String merchantId, PaymentLinkRequestDTO request) {

        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,key,environment)
                .flatMap(keys -> {
                    return paymentServiceRouter.routePayment(PaymentTypeEnum.PAYMENT_LINK,request, keys.getUserId());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error(KEYS_NOT_FOUND_FOR_MERCHANT);
                    return Mono.just(Response.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(FAILED)
                            .errors(List.of(environment+KEYS_NOT_FOUND_FOR_MERCHANT))
                            .build());
                }));    }

    @Override
    public Mono<Response<Object>> webhook(@Valid @NotNull Object request, String provider) {
        ObjectMapper mapper = new ObjectMapper();

        switch (provider){
            case "KORAPAY":{
                KoraWebhookResponse response = mapper.convertValue(request, KoraWebhookResponse.class);
                String reference = response.getData().getReference();
                return transactionRepository.findByTransactionReferenceAndProviderName(reference,"KORAPAY")
                        .flatMap(transaction -> {
                            if (response.getData().getStatus().equalsIgnoreCase("success")){
                                transaction.setTransactionStatus(Status.SUCCESSFUL);
                                transaction.setFee(response.getData().getFee());
                            } else if (response.getData().getStatus().equalsIgnoreCase("failed")) {
                                transaction.setTransactionStatus(Status.FAILED);
                            }
                            else {
                                transaction.setTransactionStatus(Status.PENDING);
                            }

                            return transactionRepository.save(transaction)
                                    .flatMap(transaction1 -> Mono.just(Response.builder()
                                            .statusCode(HttpStatus.OK.value())
                                            .status(HttpStatus.OK)
                                            .message(SUCCESSFUL)
                                                    .data(transaction1)
                                            .build()));
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            log.error("{} transaction not found with RRN {} for provider {}",response,reference,"KORAPAY");
                            return Mono.just(Response.builder()
                                            .statusCode(HttpStatus.NOT_FOUND.value())
                                            .status(HttpStatus.NOT_FOUND)
                                            .errors(List.of("transciton not found"))
                                            .message(FAILED)
                                    .build());
                        }));

            }
            case "MASTERCARD":{
                // TODO: 03/11/2023 write implementation when mastercard is up
                return Mono.empty();
            }
            case "WEMA":{
                WemaTransactionDTO wemaTransactionDTO = mapper.convertValue(request, WemaTransactionDTO.class);
                return wemaBankService.receiveTransactionNotification(wemaTransactionDTO)
                        .flatMap(wemaTransactionNotificationResponse -> Mono.just(Response.builder()
                                        .data(wemaTransactionNotificationResponse)
                                        .statusCode(HttpStatus.OK.value())
                                        .status(HttpStatus.OK)
                                        .message(SUCCESSFUL)
                                .build()))
                        .doOnError(throwable -> {
                            log.error("error receiving wema transaction notification. error is {}",throwable.getMessage());
                            throw new CustomException(Response.builder()
                                    .message(FAILED)
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .errors(List.of(throwable.getMessage()))
                                    .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                        });

            }
            default:
                throw new IllegalStateException("Unexpected value: " + provider);
        }
    }

    @Override
    public Mono<Response<Object>> doTSQ(String secret, String merchantId, String reference) {
        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,secret,environment)
                .flatMap(keys -> {
                    return transactionRepository.findByMerchantIdAndTransactionReferenceOrProviderReference(merchantId,reference,reference)
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
