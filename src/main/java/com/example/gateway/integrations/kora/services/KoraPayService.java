package com.example.gateway.integrations.kora.services;

import com.example.gateway.commons.utils.AESEncryptionUtils;
import com.example.gateway.commons.utils.HttpUtil;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.*;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferResponseDTO;
import com.example.gateway.integrations.kora.interfaces.IKoraService;
import com.example.gateway.transactions.enums.PaymentTypeEnum;
import com.example.gateway.transactions.enums.Status;
import com.example.gateway.transactions.models.Transaction;
import com.example.gateway.transactions.reporitory.TransactionRepository;
import com.example.gateway.transactions.services.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class KoraPayService implements IKoraService {
    @Value("${kora.charge.card}")
    private String chargeCardUrl;
    @Value("${kora.encryption.key}")
    private String koraPayEncryptionKey;
    @Value("${kora.authorize.card.pin}")
    private String authorizeCardEndpoint;
    @Value("${kora.verify.payment}")
    private String verifyPayment;
    @Value("${kora.transfer.url}")
    private String payWithTransferUrl;
    @Value("${kora.secret.key}")
    private String koraPaySecretKey;
    @Value("${webhook.url}")
    private String webhookUrl;
    private static final String FAILED  = "FAILED";
    private static final String SUCCESSFUL  = "SUCCESSFUL";
    private static final String PROVIDER_NAME  = "KORAPAY";
    private static final String ENCRYPTION_IV ="#$%#^%KCSWITC945";
    private static final String AUTHORIZATION ="Authorization";
    private static final String BEARER ="Bearer ";
    private final HttpUtil httpUtil;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;



    @Override
    public Mono<Response<?>> payWithCard(KoraPayWithCardRequest request, String merchantId) {
        Transaction tranLog = Transaction.builder()
                .transactionReference(request.getReference())
                .amount(request.getAmount())
                .transactionStatus(Status.ONGOING)
                .paymentType(PaymentTypeEnum.CARD)
                .userId(merchantId)
                .vestraPayReference(UUID.randomUUID().toString())
                .merchantId(merchantId)
                .providerName(PROVIDER_NAME)
                .activityStatus("transaction initiated at "+new Date())
                .pan(request.getCard().getNumber())
                .narration(request.getCustomer().getEmail())
                .build();
        return transactionService.saveTransaction(tranLog, PaymentTypeEnum.CARD,merchantId)
                .flatMap(transaction -> {
                    try {
                        return encrypt(EncryptDecryptRequest.builder()
                                .key(koraPayEncryptionKey)
                                .body(new ObjectMapper().writeValueAsString(request))
                                .build()).flatMap(stringResponse -> chargeCard(stringResponse.getData(),merchantId, request.getReference()));
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException(e));
                    }
                });

    }

    @Override
    public Mono<Response<String>> decrypt(EncryptDecryptRequest request) {
        if (request.getKey().isEmpty()||request.getBody().isEmpty()){
            return Mono.just(Response.<String>builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .status(HttpStatus.BAD_REQUEST)
                            .message(FAILED)
                            .errors(List.of("fields are mandatory"))
                    .build());
        }
        return Mono.just(Response.<String>builder()
                        .data(AESEncryptionUtils.decrypt(request.getBody(), ENCRYPTION_IV,request.getKey()))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Success")
                .build());
    }

    @Override
    public Mono<Response<String>> encrypt(EncryptDecryptRequest request) throws Exception {
        if (request.getKey().isEmpty()||request.getBody().isEmpty()){
            return Mono.just(Response.<String>builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .status(HttpStatus.BAD_REQUEST)
                    .message(FAILED)
                    .errors(List.of("fields are mandatory"))
                    .build());
        }
        return Mono.just(Response.<String>builder()
                .data(AESEncryptionUtils.encryptPayload(request.getBody(),request.getKey()))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message(SUCCESSFUL)
                .build());
    }

    @Override
    public Mono<Response<String>> chargeCard(String request, String merchantId, String reference) {
        return httpUtil.post(chargeCardUrl, Map.of("charge_data",request),Map.of(AUTHORIZATION,BEARER+ koraPayEncryptionKey),60)
                .flatMap(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()){
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(koraChargeCardResponse -> {
                                    log.info("charge card response is {}",koraChargeCardResponse);
                                    return Mono.just(Response.<String>builder()
                                            .data(koraChargeCardResponse)
                                            .message(SUCCESSFUL)
                                            .status(HttpStatus.OK)
                                            .statusCode(HttpStatus.OK.value())
                                            .build());
                                });
                    }
                    else {
                        log.info("response not successful");
                        return clientResponse.bodyToMono(Map.class)
                                .flatMap(map -> Mono.just(Response.<String>builder()
                                        .message(FAILED)
                                        .errors(List.of(map.toString()))
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .build()));
                    }
                });
    }


    @Override
    public Mono<Response<Object>> authorizeCard(AuthorizeCardRequestPin request, String merchantId) {
        return transactionRepository.findByTransactionReferenceAndMerchantId(request.getTransactionReference(),merchantId)
                .flatMap(transaction -> httpUtil.post(authorizeCardEndpoint,request,new HashMap<>(),90)
                        .flatMap(clientResponse -> {
                            if (clientResponse.statusCode().is2xxSuccessful()){
                                return clientResponse.bodyToMono(TransactionRootResponse.class)
                                        .flatMap(transactionRootResponse -> {
                                            if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getAuthModel().equalsIgnoreCase("NO_AUTH")){
                                                transaction.setTransactionStatus(Status.SUCCESSFUL);
                                            } else if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("processing")) {
                                                transaction.setTransactionStatus(Status.PROCESSING);

                                            }
                                            transaction.setNarration(transactionRootResponse.getData().getResponseMessage());
                                            transaction.setProviderReference(transactionRootResponse.getData().getPaymentReference());

                                            return transactionRepository.save(transaction)
                                                    .flatMap(transaction1 -> {
                                                        return Mono.just(Response.builder()
                                                                .data(transactionRootResponse)
                                                                .message(SUCCESSFUL)
                                                                .statusCode(HttpStatus.OK.value())
                                                                .status(HttpStatus.OK)
                                                                .build());
                                                    });

                                        });
                            }
                            else {
                                return clientResponse.bodyToMono(Map.class)
                                        .flatMap(map -> Mono.just(Response.builder()
                                                .data(map)
                                                .message(FAILED)
                                                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .build()));
                            }
                        })).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .message(FAILED)
                        .errors(List.of("Transaction not found"))
                        .build())));


    }

    @Override
    public Mono<Response<Object>> authorizeCardOtp(AuthorizeCardRequestOTP request, String merchantId) {
        return transactionRepository.findByTransactionReferenceAndMerchantId(request.getTransactionReference(),merchantId)
                .flatMap(transaction -> {
                    return httpUtil.post(authorizeCardEndpoint,request,new HashMap<>(),90)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(TransactionRootResponse.class)
                                            .flatMap(transactionRootResponse -> {
                                                if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getAuthModel().equalsIgnoreCase("NO_AUTH")){
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                } else if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("processing")) {
                                                    transaction.setTransactionStatus(Status.PROCESSING);

                                                }
                                                transaction.setNarration(transactionRootResponse.getData().getResponseMessage());
                                                transaction.setProviderReference(transactionRootResponse.getData().getPaymentReference());

                                                return transactionRepository.save(transaction)
                                                        .flatMap(transaction1 -> {
                                                            return Mono.just(Response.builder()
                                                                    .data(transactionRootResponse)
                                                                    .message(SUCCESSFUL)
                                                                    .statusCode(HttpStatus.OK.value())
                                                                    .status(HttpStatus.OK)
                                                                    .build());
                                                        });

                                            });
                                }
                                else {
                                    return clientResponse.bodyToMono(Map.class)
                                            .flatMap(map -> Mono.just(Response.builder()
                                                    .data(map)
                                                    .message(FAILED)
                                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                    .build()));
                                }
                            });
                }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .message(FAILED)
                        .errors(List.of("Transaction not found"))
                        .build())));
    }

    @Override
    public Mono<Response<Object>> authorizeCardAvs(AuthorizeCardRequestAVS request, String merchantId) {
        return transactionRepository.findByTransactionReferenceAndMerchantId(request.getTransactionReference(),merchantId)
                .flatMap(transaction -> {
                    return httpUtil.post(authorizeCardEndpoint,request,new HashMap<>(),90)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(TransactionRootResponse.class)
                                            .flatMap(transactionRootResponse -> {
                                                if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getAuthModel().equalsIgnoreCase("NO_AUTH")){
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                } else if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("processing")) {
                                                    transaction.setTransactionStatus(Status.PROCESSING);

                                                }
                                                transaction.setNarration(transactionRootResponse.getData().getResponseMessage());
                                                transaction.setProviderReference(transactionRootResponse.getData().getPaymentReference());

                                                return transactionRepository.save(transaction)
                                                        .flatMap(transaction1 -> {
                                                            return Mono.just(Response.builder()
                                                                    .data(transactionRootResponse)
                                                                    .message(SUCCESSFUL)
                                                                    .statusCode(HttpStatus.OK.value())
                                                                    .status(HttpStatus.OK)
                                                                    .build());
                                                        });

                                            });
                                }
                                else {
                                    return clientResponse.bodyToMono(Map.class)
                                            .flatMap(map -> Mono.just(Response.builder()
                                                    .data(map)
                                                    .message(FAILED)
                                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                    .build()));
                                }
                            });
                }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .message(FAILED)
                        .errors(List.of("Transaction not found"))
                        .build())));
    }

    @Override
    public Mono<Response<Object>> authorizeCardPhone(AuthorizeCardRequestPhone request, String merchantId) {
        return transactionRepository.findByTransactionReferenceAndMerchantId(request.getTransactionReference(),merchantId)
                .flatMap(transaction -> {
                    return httpUtil.post(authorizeCardEndpoint,request,new HashMap<>(),90)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(TransactionRootResponse.class)
                                            .flatMap(transactionRootResponse -> {
                                                if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getAuthModel().equalsIgnoreCase("NO_AUTH")){
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                } else if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("processing")) {
                                                    transaction.setTransactionStatus(Status.PROCESSING);

                                                }
                                                transaction.setNarration(transactionRootResponse.getData().getResponseMessage());
                                                transaction.setProviderReference(transactionRootResponse.getData().getPaymentReference());

                                                return transactionRepository.save(transaction)
                                                        .flatMap(transaction1 -> {
                                                            return Mono.just(Response.builder()
                                                                    .data(transactionRootResponse)
                                                                    .message(SUCCESSFUL)
                                                                    .statusCode(HttpStatus.OK.value())
                                                                    .status(HttpStatus.OK)
                                                                    .build());
                                                        });

                                            });
                                }
                                else {
                                    return clientResponse.bodyToMono(Map.class)
                                            .flatMap(map -> Mono.just(Response.builder()
                                                    .data(map)
                                                    .message(FAILED)
                                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                    .build()));
                                }
                            });
                }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .message(FAILED)
                        .errors(List.of("Transaction not found"))
                        .build())));
    }

    @Override
    public Mono<Response<Object>> verifyTransaction(String reference, String merchantId) {
        return transactionRepository.findByTransactionReferenceAndMerchantId(reference,merchantId)
                .flatMap(transaction -> {
                    return httpUtil.get(verifyPayment.concat(reference),Map.of("Authorization","Bearer "+ koraPayEncryptionKey),90)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(TransactionRootResponse.class)
                                            .flatMap(transactionRootResponse -> {
                                                log.info("transaction response is {}",transactionRootResponse);
                                                if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getAuthModel().equalsIgnoreCase("NO_AUTH")){
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                } else if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("processing")) {
                                                    transaction.setTransactionStatus(Status.PROCESSING);

                                                }
                                                transaction.setNarration(transactionRootResponse.getData().getResponseMessage());
                                                transaction.setProviderReference(transactionRootResponse.getData().getPaymentReference());

                                                return transactionRepository.save(transaction)
                                                        .flatMap(transaction1 -> {
                                                            return Mono.just(Response.builder()
                                                                    .data(transactionRootResponse)
                                                                    .message(SUCCESSFUL)
                                                                    .statusCode(HttpStatus.OK.value())
                                                                    .status(HttpStatus.OK)
                                                                    .build());
                                                        });
                                            });
                                }
                                else {
                                    return clientResponse.bodyToMono(Map.class)
                                            .flatMap(transactionRootResponse -> {
                                                log.info("transaction response is {}",transactionRootResponse);
                                                return Mono.just(Response.builder()
                                                        .data(transactionRootResponse)
                                                        .message(FAILED)
                                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .build());
                                            });
                                }
                            });
                }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                                .status(HttpStatus.NOT_FOUND)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .message(FAILED)
                                .errors(List.of("Transaction not found"))
                        .build())));

    }

    @Override
    public Mono<Response<Object>> payWithTransfer(PayWithTransferDTO request,String merchantId) {
        request.setNotificationUrl(webhookUrl);
        Transaction tranLog = Transaction.builder()
                .transactionReference(request.getReference())
                .amount(request.getAmount())
                .transactionStatus(Status.ONGOING)
                .pan("TRANSFER")
                .cardScheme("TRANSFER")
                .paymentType(PaymentTypeEnum.TRANSFER)
                .userId(merchantId)
                .vestraPayReference(UUID.randomUUID().toString())
                .merchantId(merchantId)
                .providerName(PROVIDER_NAME)
                .activityStatus("transaction initiated at "+new Date())
                .narration(request.getCustomer().getEmail())
                .build();
        return transactionService.saveTransaction(tranLog,PaymentTypeEnum.TRANSFER, merchantId)
                .flatMap(transaction -> {
                    return httpUtil.post(payWithTransferUrl,request,Map.of("Authorization", "Bearer "+koraPaySecretKey),90)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(PayWithTransferResponseDTO.class)
                                            .flatMap(payWithTransferResponseDTO -> transactionRepository.findByTransactionReferenceAndMerchantId(request.getReference(),merchantId)
                                                    .flatMap(transaction1 -> {
                                                        transaction1.setTransactionStatus(Status.PROCESSING);
                                                        transaction1.setNarration(payWithTransferResponseDTO.getData().getNarration());
                                                        transaction1.setPan(payWithTransferResponseDTO.getData().getBankAccount().getAccountName()+ " " +payWithTransferResponseDTO.getData().getBankAccount().getBankName());
                                                        return transactionRepository.save(transaction1)
                                                                .flatMap(transaction2 -> Mono.just(Response.builder()
                                                                        .status(HttpStatus.OK)
                                                                        .statusCode(HttpStatus.OK.value())
                                                                        .message(SUCCESSFUL)
                                                                        .data(payWithTransferResponseDTO)
                                                                        .build()));
                                                    }));
                                }

                                return clientResponse.bodyToMono(Map.class)
                                        .flatMap(map -> Mono.just(Response.builder()
                                                .data(map)
                                                .message(FAILED)
                                                .statusCode(HttpStatus.EXPECTATION_FAILED.value())
                                                .status(HttpStatus.EXPECTATION_FAILED)
                                                .build()));
                            });
                });

    }


}
