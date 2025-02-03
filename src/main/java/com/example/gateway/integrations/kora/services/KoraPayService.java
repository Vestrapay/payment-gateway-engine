package com.example.gateway.integrations.kora.services;

import com.example.gateway.api.card.dtos.CardPaymentRequestDTO;
import com.example.gateway.api.card.interfaces.ICardService;
import com.example.gateway.api.transfer.dtos.TransferPaymentRequestDTO;
import com.example.gateway.api.transfer.interfaces.ITransferService;
import com.example.gateway.commons.charge.enums.ChargeCategory;
import com.example.gateway.commons.charge.enums.PaymentMethod;
import com.example.gateway.commons.charge.service.ChargeService;
import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.notificatioin.NotificationService;
import com.example.gateway.commons.utils.*;
import com.example.gateway.integrations.kora.dtos.card.*;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferResponseDTO;
import com.example.gateway.commons.transactions.enums.PaymentTypeEnum;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.models.Transaction;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import com.example.gateway.commons.transactions.services.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
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
public class KoraPayService implements ITransferService, ICardService {
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
    @Value("${notification.email}")
    private String vestraPayEmail;
    @Value("${kora.tsq.url}")
    private String tsqUrl;
    private static final String FAILED  = "FAILED";
    private static final String SUCCESSFUL  = "SUCCESSFUL";
    private static final String PROVIDER_NAME  = "KORAPAY";
    private static final String ENCRYPTION_IV ="#$%#^%KCSWITC945";
    private static final String AUTHORIZATION ="Authorization";
    private static final String BEARER ="Bearer ";
    private final HttpUtil httpUtil;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final ChargeService chargeService;
    private final NotificationService notificationService;

    @Override
    public Mono<Response<?>> payWithCard(CardPaymentRequestDTO cardPaymentRequestDTO, String merchantId, String userId) {
        KoraPayWithCardRequest request = ObjectMapperUtil.toKoraPayCardDTO(cardPaymentRequestDTO, merchantId);
        Customer customer = new Customer(cardPaymentRequestDTO.getCard().getName(),cardPaymentRequestDTO.getCustomerDetails().getEmail()==null?"no-reply@vestrapay.com":cardPaymentRequestDTO.getCustomerDetails().getEmail()); //refactor to get email from environment
        request.setCustomer(customer);
        String vestrapayReference = UUID.randomUUID().toString();
        Transaction tranLog = Transaction.builder()
                .transactionReference(request.getReference())
                .amount(request.getAmount())
                .transactionStatus(Status.ONGOING)
                .paymentType(PaymentTypeEnum.CARD)
                .cardScheme(PaymentUtils.detectCardScheme(request.getCard().getNumber()))
                .vestraPayReference(request.getMetadata().getInternalRef())
                .userId(userId)
                .currency(request.getCurrency())
                .vestraPayReference(vestrapayReference)
                .merchantId(merchantId)
                .providerName(PROVIDER_NAME)
                .activityStatus("transaction initiated at "+new Date())
                .pan(request.getCard().getNumber())
                .narration("Card Payment from "+request.getCustomer().getName())
                .metaData(Objects.isNull(request.getMetadata())?null:new Gson().toJson(request.getMetadata()))
                .build();

        tranLog.setCurrency(request.getCurrency().substring(0,3));
        log.info("incoming transaction log {}",tranLog);
        return chargeService.getPaymentCharge(tranLog.getMerchantId(), PaymentMethod.CARD, ChargeCategory.PAY_IN,tranLog.getAmount(),request.getCurrency())
                .flatMap(fee -> {
                    tranLog.setFee(fee);
                    return transactionService.saveTransaction(tranLog, PaymentTypeEnum.CARD,merchantId)
                            .flatMap(transaction -> {
                                try {
                                    request.setRedirectUrl(webhookUrl.concat("KORAPAY"));
                                    return encrypt(EncryptDecryptRequest.builder()
                                            .key(koraPayEncryptionKey)
                                            .body(new ObjectMapper().writeValueAsString(request))
                                            .build()).flatMap(stringResponse -> chargeCard(stringResponse.getData(),merchantId, request.getReference()));
                                } catch (Exception e) {
                                    return Mono.error(new RuntimeException(e));
                                }
                            }).switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(Response.builder()
                                    .statusCode(HttpStatus.CONFLICT.value())
                                    .message("Attempt for duplicate request forbidden")
                                    .build(), HttpStatus.CONFLICT))));
                });

    }


    private Mono<Response<String>> decrypt(EncryptDecryptRequest request) {
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

    private Mono<Response<String>> encrypt(EncryptDecryptRequest request) throws Exception {
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

    private Mono<Response<?>> chargeCard(String request, String merchantId, String reference) {
        return httpUtil.post(chargeCardUrl, Map.of("charge_data",request),Map.of(AUTHORIZATION,BEARER+ koraPaySecretKey),60)
                .flatMap(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()){
                        return clientResponse.bodyToMono(KoraChargeCardResponse.class)
                                .flatMap(koraChargeCardResponse -> {
                                    log.info("charge card response is {}",koraChargeCardResponse);
                                    return transactionRepository.findByTransactionReferenceAndMerchantId(reference,merchantId)
                                            .flatMap(transaction -> {
                                                transaction.setProviderReference(koraChargeCardResponse.getData().getTransactionReference());

                                                if (koraChargeCardResponse.isStatus()&& koraChargeCardResponse.getData().getStatus().equalsIgnoreCase("processing")){
                                                    transaction.setTransactionStatus(Status.ONGOING);
                                                } else if (koraChargeCardResponse.isStatus()&& koraChargeCardResponse.getData().getStatus().equalsIgnoreCase("success")) {
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                }
                                                else
                                                    transaction.setTransactionStatus(Status.FAILED);

                                                return transactionService.updateTransaction(transaction)
                                                        .flatMap(transaction1 -> {
                                                            koraChargeCardResponse.setTransaction(transaction);
                                                            notificationService.postNotification(transaction1);
                                                            return Mono.just(Response.builder()
                                                                    .data(koraChargeCardResponse)
                                                                    .message(SUCCESSFUL)
                                                                    .status(HttpStatus.OK)
                                                                    .statusCode(HttpStatus.OK.value())
                                                                    .build());
                                                        });

                                            });

                                });
                    }
                    else {
                        log.info("response not successful");
                        return clientResponse.bodyToMono(Map.class)
                                .flatMap(map ->{
                                    return transactionRepository.findByTransactionReferenceAndMerchantId(reference,merchantId)
                                                    .flatMap(transaction -> {
                                                        transaction.setTransactionStatus(Status.FAILED);
                                                        return transactionRepository.save(transaction)
                                                                .flatMap(transaction1 -> {
                                                                    notificationService.postNotification(transaction1);
                                                                    return Mono.just(Response.builder()
                                                                            .message(FAILED)
                                                                                    .data(transaction1)
                                                                            .errors(List.of(map.toString()))
                                                                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                                            .build());
                                                                });
                                                    }).onErrorResume(throwable -> {
                                                        log.error(throwable.getMessage());
                                                return Mono.just(Response.builder()
                                                        .message(FAILED)
                                                        .data(null)
                                                        .errors(List.of(map.toString()))
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .build());
                                            });

                                });
                    }
                });
    }


    @Override
    public Mono<Response<Object>> authorizeCard(AuthorizeCardRequestPin request, String merchantId, String userId) {
        return transactionRepository.findByMerchantIdAndTransactionReferenceOrProviderReferenceAndUserId(merchantId,request.getTransactionReference(),request.getTransactionReference(),userId)
                .flatMap(transaction -> {
                    request.setTransactionReference(transaction.getProviderReference());
                    return httpUtil.post(authorizeCardEndpoint,request,Map.of(AUTHORIZATION,BEARER+ koraPaySecretKey),90)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(TransactionRootResponse.class)
                                            .flatMap(transactionRootResponse -> {
                                                log.info("response from authorize card is {}",transactionRootResponse);
                                                if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("success")){
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                } else if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("processing")) {
                                                    transaction.setTransactionStatus(Status.PROCESSING);

                                                }
                                                transaction.setProviderReference(transactionRootResponse.getData().getTransactionReference());

                                                return transactionService.updateTransaction(transaction)
                                                        .flatMap(transaction1 -> {
                                                            notificationService.postNotification(transaction1);
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
                                            .flatMap(map -> {
                                                log.error("error response is {}",map);
                                                return Mono.just(Response.builder()
                                                        .data(map)
                                                        .message(FAILED)
                                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .build());
                                            });
                                }
                            }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                                    .status(HttpStatus.NOT_FOUND)
                                    .statusCode(HttpStatus.NOT_FOUND.value())
                                    .message(FAILED)
                                    .errors(List.of("Transaction not found"))
                                    .build())));
                }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .message(FAILED)
                        .errors(List.of("Transaction not found"))
                        .build())));


    }

    @Override
    public Mono<Response<Object>> authorizeCardOtp(AuthorizeCardRequestOTP request, String merchantId, String userId) {
        return transactionRepository.findByMerchantIdAndTransactionReferenceOrProviderReferenceAndUserId(merchantId,request.getTransactionReference(),request.getTransactionReference(),userId)
                .flatMap(transaction -> {
                    request.setTransactionReference(transaction.getProviderReference());
                    return httpUtil.post(authorizeCardEndpoint,request,Map.of(AUTHORIZATION,BEARER+ koraPaySecretKey),90)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(TransactionRootResponse.class)
                                            .flatMap(transactionRootResponse -> {
                                                log.info("response from authorizeCard OTP is {}",transactionRootResponse);

                                                if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("success")){
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                } else if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("processing")) {
                                                    transaction.setTransactionStatus(Status.PROCESSING);

                                                }
//                                                transaction.setNarration(transactionRootResponse.getData().getResponseMessage());
                                                transaction.setProviderReference(transactionRootResponse.getData().getTransactionReference());

                                                return transactionService.updateTransaction(transaction)
                                                        .flatMap(transaction1 -> {
                                                            notificationService.postNotification(transaction1);
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
                                            .flatMap(map -> {
                                                log.error("error response is {}",map);
                                                return Mono.just(Response.builder()
                                                        .data(map)
                                                        .message(FAILED)
                                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .build());
                                            });
                                }
                            }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                                    .status(HttpStatus.NOT_FOUND)
                                    .statusCode(HttpStatus.NOT_FOUND.value())
                                    .message(FAILED)
                                    .errors(List.of("Transaction not found"))
                                    .build())));
                }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                .status(HttpStatus.NOT_FOUND)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .message(FAILED)
                .errors(List.of("Transaction not found"))
                .build())));
    }

    @Override
    public Mono<Response<Object>> authorizeCardAvs(AuthorizeCardRequestAVS request, String merchantId, String userId) {
        return transactionRepository.findByMerchantIdAndTransactionReferenceOrProviderReferenceAndUserId(merchantId,request.getTransactionReference(),request.getTransactionReference(),userId)
                .flatMap(transaction -> {
                    request.setTransactionReference(transaction.getProviderReference());
                    return httpUtil.post(authorizeCardEndpoint,request,Map.of(AUTHORIZATION,BEARER+ koraPaySecretKey),90)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(TransactionRootResponse.class)
                                            .flatMap(transactionRootResponse -> {
                                                log.info("response from authorizeCard AVS is {}",transactionRootResponse);

                                                if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("success")){
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                } else if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("processing")) {
                                                    transaction.setTransactionStatus(Status.PROCESSING);

                                                }
                                                transaction.setProviderReference(transactionRootResponse.getData().getTransactionReference());

                                                return transactionService.updateTransaction(transaction)
                                                        .flatMap(transaction1 -> {
                                                            notificationService.postNotification(transaction1);

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
                                            .flatMap(map -> {
                                                log.error("error response is {}",map);
                                                return Mono.just(Response.builder()
                                                        .data(map)
                                                        .message(FAILED)
                                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .build());
                                            });
                                }
                            }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                                    .status(HttpStatus.NOT_FOUND)
                                    .statusCode(HttpStatus.NOT_FOUND.value())
                                    .message(FAILED)
                                    .errors(List.of("Transaction not found"))
                                    .build())));
                }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .message(FAILED)
                        .errors(List.of("Transaction not found"))
                        .build())));

    }

    @Override
    public Mono<Response<Object>> authorizeCardPhone(AuthorizeCardRequestPhone request, String merchantId, String userId) {
        return transactionRepository.findByMerchantIdAndTransactionReferenceOrProviderReferenceAndUserId(merchantId,request.getTransactionReference(),request.getTransactionReference(),userId)
                .flatMap(transaction -> {
                    request.setTransactionReference(transaction.getProviderReference());
                    return httpUtil.post(authorizeCardEndpoint,request,Map.of(AUTHORIZATION,BEARER+ koraPaySecretKey),90)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(TransactionRootResponse.class)
                                            .flatMap(transactionRootResponse -> {
                                                log.info("response from authorizeCard Phone is {}",transactionRootResponse);

                                                if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("success")){
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                } else if (transactionRootResponse.isStatus() && transactionRootResponse.getData().getStatus().equalsIgnoreCase("processing")) {
                                                    transaction.setTransactionStatus(Status.PROCESSING);

                                                }
                                                transaction.setProviderReference(transactionRootResponse.getData().getTransactionReference());

                                                return transactionService.updateTransaction(transaction)
                                                        .flatMap(transaction1 -> {
                                                            notificationService.postNotification(transaction1);

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
                                            .flatMap(map -> {
                                                log.error("error response is {}",map);
                                                return Mono.just(Response.builder()
                                                        .data(map)
                                                        .message(FAILED)
                                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .build());
                                            });
                                }
                            }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                                    .status(HttpStatus.NOT_FOUND)
                                    .statusCode(HttpStatus.NOT_FOUND.value())
                                    .message(FAILED)
                                    .errors(List.of("Transaction not found"))
                                    .build())));
                }).switchIfEmpty(Mono.defer(() -> Mono.just(Response.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .message(FAILED)
                        .errors(List.of("Transaction not found"))
                        .build())));
    }

    @Override
    public String getName() {
        return "KORAPAY";
    }

    public Mono<Response<Object>> verifyTransaction(String reference, String merchantId, String userId) {
        return transactionRepository.findByMerchantIdAndTransactionReferenceOrProviderReferenceAndUserId(merchantId,reference,reference,userId)
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
                                                transaction.setProviderReference(transactionRootResponse.getData().getTransactionReference());

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
    public Mono<Response<Object>> payWithTransfer(TransferPaymentRequestDTO transferPaymentRequestDTO, String merchantId, String userId) {
        PayWithTransferDTO request = ObjectMapperUtil.toKoraPayTransferDTO(transferPaymentRequestDTO);

        request.setNotificationUrl(webhookUrl.concat(PROVIDER_NAME));
        Transaction tranLog = Transaction.builder()
                .transactionReference(request.getReference())
                .amount(request.getAmount())
                .transactionStatus(Status.ONGOING)
                .pan("TRANSFER")
                .cardScheme("TRANSFER")
                .paymentType(PaymentTypeEnum.TRANSFER)
                .userId(userId)
                .currency(request.getCurrency())
                .vestraPayReference(UUID.randomUUID().toString())
                .merchantId(merchantId)
                .providerName(PROVIDER_NAME)
                .activityStatus("transaction initiated at "+new Date())
                .narration(request.getCustomer().getEmail())
                .userId(request.getCustomer().getEmail())
                .metaData(request.getMetaData())
                .build();
        request.getCustomer().setEmail(vestraPayEmail);
        return chargeService.getPaymentCharge(tranLog.getMerchantId(),PaymentMethod.TRANSFER,ChargeCategory.PAY_IN,tranLog.getAmount(),request.getCurrency())
                .flatMap(fee -> {
                    tranLog.setFee(fee);
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
                                                                    payWithTransferResponseDTO.getData().setTransaction(transaction1);
                                                                    return transactionRepository.save(transaction1)
                                                                            .flatMap(transaction2 -> {
                                                                                if (Objects.nonNull(payWithTransferResponseDTO.getData().getFee()))
                                                                                    payWithTransferResponseDTO.getData().setFee(transaction1.getFee());
                                                                                return Mono.just(Response.builder()
                                                                                        .status(HttpStatus.OK)
                                                                                        .statusCode(HttpStatus.OK.value())
                                                                                        .message(SUCCESSFUL)
                                                                                        .data(payWithTransferResponseDTO)
                                                                                        .build());
                                                                            });
                                                                }));
                                            }

                                            return clientResponse.bodyToMono(Map.class)
                                                    .flatMap(map -> {
                                                        log.error("error performing pay with transfer for id {} error is {}", tranLog.getTransactionReference(),map);
                                                        Transaction tranlog= (Transaction)transaction;
                                                        tranlog.setTransactionStatus(Status.FAILED);
                                                        return transactionService.updateTransaction(tranlog)
                                                                .flatMap(transaction1 -> {
                                                                    map.put("transaction",transaction1);
                                                                    return Mono.just(Response.builder()
                                                                            .status(HttpStatus.EXPECTATION_FAILED)
                                                                            .statusCode(HttpStatus.EXPECTATION_FAILED.value())
                                                                            .message(FAILED)
                                                                            .data(map)
                                                                            .build());
                                                                });
                                                    });
                                        });
                            });
                });


    }

    public Mono<Transaction> doTSQ(Transaction transaction) {
        return httpUtil.get(tsqUrl.concat(transaction.getTransactionReference()),Map.of("Authorization", "Bearer "+koraPaySecretKey),90)
                .flatMap(clientResponse -> {
                    log.info("ClientResponse code:: {}", clientResponse.statusCode().value());
                    if (clientResponse.statusCode().is2xxSuccessful()){
                        return clientResponse.bodyToMono(PayWithTransferResponseDTO.class)
                                .flatMap(payWithTransferResponseDTO -> {
                                    if (payWithTransferResponseDTO.isStatus() && Objects.nonNull(payWithTransferResponseDTO.getData())){
                                        if (Objects.nonNull(payWithTransferResponseDTO.getData().getStatus())){
                                            String status = payWithTransferResponseDTO.getData().getStatus();
                                            if (status.equalsIgnoreCase("success")){
                                                transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                notificationService.postNotification(transaction);
                                            }
                                            if (status.equalsIgnoreCase("failed"))
                                                transaction.setTransactionStatus(Status.FAILED);
                                            if (status.equalsIgnoreCase("expired"))
                                                transaction.setTransactionStatus(Status.EXPIRED);
                                            transactionRepository.save(transaction).subscribe();
                                        }
                                    }
                                    log.info("response for TSQ is {}", payWithTransferResponseDTO);
                                    return Mono.just(transaction);

                                });
                    }

                    return clientResponse.bodyToMono(Object.class)
                            .flatMap(map -> {
                                log.error("error performing transfer TSQ. error is {}",map.toString());
//                                transaction.setTransactionStatus(Status.FAILED);
                                return Mono.just(transaction);
                            });
                }).onErrorResume(throwable -> {
//                    transaction.setTransactionStatus(Status.FAILED);
                    return Mono.just(transaction);
                });


    }



}
