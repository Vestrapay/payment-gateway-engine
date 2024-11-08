package com.example.gateway.integrations.paymentlink.service;

import com.example.gateway.commons.keys.repository.KeysRepository;
import com.example.gateway.commons.merchants.service.ValidateUserKycService;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.paymentlink.dto.PaymentLinkDTO;
import com.example.gateway.integrations.paymentlink.entity.PaymentLink;
import com.example.gateway.api.paymentlink.interfaces.IPaymentLinkService;
import com.example.gateway.integrations.paymentlink.repository.PaymentLinkRepository;
import com.example.gateway.integrations.paymentlink.utils.ResponseDTO;
import com.example.gateway.commons.transactions.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentLinkService implements IPaymentLinkService {
    private final PaymentLinkRepository paymentLinkRepository;
    private final KeysRepository keysRepository;
    private static final String SUCCESS = "SUCCESSFUL";
    private static final String FAILED = "FAILED";
    @Value("${vestrapay.url}")
    String baseUrl;

    @Value("${server.environment}")
    String environment;
    private final ValidateUserKycService validateUserKycService;
    @Override
    public Mono<Response<Object>> generatePaymentLink(String merchantId, String secret, PaymentLinkDTO request, String userId) {
        return validateUserKycService.isKycComplete(merchantId)
                .flatMap(aBoolean -> {
                    if (aBoolean){
                        String reference = UUID.randomUUID().toString();
                        String path = request.getCustomizedLink().isEmpty() ?reference : request.getCustomizedLink();
                        String finalPath = "";
                        if (path.startsWith("/")){
                            finalPath = path.substring(1);
                        }
                        else
                            finalPath = path;

                        String finalPath1 = finalPath;
                        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,secret,environment)
                                .flatMap(keys -> {
                                    return paymentLinkRepository.findByMerchantIdAndPath(merchantId, finalPath1)
                                            .flatMap(paymentLink -> {
                                                log.error("payment link already exist with invoice number for merchant");
                                                return Mono.just(Response.builder()
                                                        .statusCode(HttpStatus.CONFLICT.value())
                                                        .status(HttpStatus.CONFLICT)
                                                        .message(SUCCESS)
                                                        .errors(List.of("payment link already exists",paymentLink.getLink()))
                                                        .build());
                                            })
                                            .switchIfEmpty(Mono.defer(() -> {

                                                PaymentLink paymentLink = PaymentLink.builder()
                                                        .uuid(UUID.randomUUID().toString())
                                                        .link(baseUrl.concat("/"+finalPath1))
                                                        .path(finalPath1)
                                                        .userId(userId)
                                                        .amount(request.getAmount())
                                                        .invoiceId(request.getInvoiceId())
                                                        .transactionId(reference)
                                                        .description(request.getDescription())
                                                        .customerEmail(request.getCustomer().getEmail())
                                                        .customerName(request.getCustomer().getName())
                                                        .params(request.getAdditionalData().toString())
                                                        .merchantId(merchantId)
                                                        .status(Status.INITIATED)
                                                        .expiryDate(LocalDateTime.now().plusDays(request.getDaysActive()))
                                                        .build();
                                                log.info("payment link DTO {}",paymentLink);
                                                return paymentLinkRepository.save(paymentLink)
                                                        .flatMap(paymentLink1 -> {
                                                            return Mono.just(Response.builder()
                                                                    .data(paymentLink1)
                                                                    .message(SUCCESS)
                                                                    .status(HttpStatus.CREATED)
                                                                    .statusCode(HttpStatus.CREATED.value())
                                                                    .build());
                                                        });
                                            }));

                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.error("keys not found for user for environment {}",environment);
                                    return Mono.just(Response.builder()
                                            .status(HttpStatus.BAD_REQUEST)
                                            .statusCode(HttpStatus.BAD_REQUEST.value())
                                            .message(FAILED)
                                            .errors(List.of(environment+" keys not found for merchant"))
                                            .build());
                                }));
                    }
                    log.error("KYC incomplete for merchant {}",merchantId);
                    return Mono.just(Response.builder()
                            .status(HttpStatus.UNAUTHORIZED)
                            .errors(List.of("Merchant KYC not complete"))
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .data("Merchant KYC not complete")
                            .message(FAILED)
                            .build());
                });

    }

    @Override
    public Mono<Response<Object>> viewLinkStatus(String merchantId, String secret, String linkId, String userId) {
        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,secret,environment).flatMap(keys -> {
            return paymentLinkRepository.findByMerchantIdAndUuid(merchantId,linkId)
                    .flatMap(paymentLink -> {
                        return Mono.just(Response.builder()
                                        .statusCode(HttpStatus.OK.value())
                                        .status(HttpStatus.OK)
                                        .data(paymentLink)
                                        .message(SUCCESS)
                                .build());
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.error("payment link does not exist with link id for merchant");
                        return Mono.just(Response.builder()
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .status(HttpStatus.BAD_REQUEST)
                                .message(SUCCESS)
                                .errors(List.of("payment link does not exists",linkId))
                                .build());
                    }));

        }).switchIfEmpty(Mono.defer(() -> {
            log.error("keys not found for user in environment {}",environment);
            return Mono.just(Response.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(FAILED)
                    .errors(List.of("keys not found for merchant"))
                    .build());
        }));

    }

    @Override
    public Mono<Response<Object>> fetchAllLinksForMerchant(String merchantId, String secret, String userId) {
        return keysRepository.findByUserIdAndSecretKeyAndKeyUsage(merchantId,secret,environment).flatMap(keys -> {
            return paymentLinkRepository.findByMerchantId(merchantId)
                    .collectList()
                    .flatMap(paymentLinks -> {
                        return Mono.just(Response.builder()
                                        .message(SUCCESS)
                                        .data(paymentLinks)
                                        .statusCode(HttpStatus.OK.value())
                                        .status(HttpStatus.OK)
                                .build());
                    });
        }).switchIfEmpty(Mono.defer(() -> {
            log.error("keys not found for user");
            return Mono.just(Response.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(FAILED)
                    .errors(List.of("keys not found for merchant"))
                    .build());
        }));
    }

    @Override
    public Mono<ResponseDTO<Object>> getPaymentLinkDetails(String linkId) {
        // TODO: 10/12/2023 implement send FE base URL to send email notification to customer
        return paymentLinkRepository.findByPathAndStatus(linkId,Status.INITIATED)
                .collectList()
                .flatMap(paymentLink -> {
                    if (paymentLink.isEmpty()){
                        return Mono.just(ResponseDTO.builder()
                                .status(HttpStatus.OK)
                                .statusCode(HttpStatus.OK.value())
                                .data(paymentLink)
                                .message(SUCCESS)
                                .build());
                    }
                    else{
                        return keysRepository.findByUserIdAndKeyUsage(paymentLink.get(0).getMerchantId(), environment.toUpperCase())
                                .flatMap(keys -> {
                                    return Mono.just(ResponseDTO.builder()
                                            .status(HttpStatus.OK)
                                            .statusCode(HttpStatus.OK.value())
                                            .data(paymentLink)
                                                    .keys(keys)
                                            .message(SUCCESS)
                                            .build());
                                }).switchIfEmpty(Mono.defer(() -> {
                                    return Mono.just(ResponseDTO.builder()
                                            .status(HttpStatus.NOT_FOUND)
                                            .errors(List.of(environment+" keys not found for merchant. generate "+ environment+" keys"))
                                            .statusCode(HttpStatus.NOT_FOUND.value())
                                            .message(FAILED)
                                            .build());
                                }));
                    }

                }).switchIfEmpty(Mono.defer(() -> {
                    return Mono.just(ResponseDTO.builder()
                            .status(HttpStatus.NOT_FOUND)
                                    .errors(List.of("payment link not found"))
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .message(FAILED)
                            .build());
                }));
    }
}
