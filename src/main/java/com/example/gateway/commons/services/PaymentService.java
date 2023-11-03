package com.example.gateway.commons.services;

import com.example.gateway.commons.dto.card.CardPaymentRequestDTO;
import com.example.gateway.commons.dto.card.CardPaymentResponseDTO;
import com.example.gateway.commons.dto.paymentlink.PaymentLinkRequestDTO;
import com.example.gateway.commons.dto.transfer.TransferPaymentRequestDTO;
import com.example.gateway.commons.dto.ussd.USSDPaymentRequestDTO;
import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.interfaces.IPaymentService;
import com.example.gateway.commons.keys.enums.KeyUsage;
import com.example.gateway.commons.keys.repository.KeysRepository;
import com.example.gateway.commons.utils.AuthorizationKeyUtil;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.sandbox.SandboxEnvironment;
import com.example.gateway.transactions.enums.PaymentTypeEnum;
import com.example.gateway.transactions.enums.Status;
import com.example.gateway.transactions.models.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements IPaymentService {
    private final KeysRepository keysRepository;
    private static final String FAILED  = "Failed";
    private static final String SUCCESSFUL  = "Successful";
    private final SandboxEnvironment sandboxEnvironment;
    private final PaymentServiceRouter paymentServiceRouter;
    private static final String PROVIDER  = "KORAPAY";
    @Override
    public Mono<Response<?>> payWithCard(String key, String merchantId, CardPaymentRequestDTO request) {

        return keysRepository.findByUserIdAndSecretKey(merchantId,key)
                .flatMap(keys -> {
                    if (keys.getKeyUsage().equals(KeyUsage.TEST))
                        return sandboxEnvironment.getResponse(CardPaymentResponseDTO.builder().build());
                    else
                        return paymentServiceRouter.routePayment(PaymentTypeEnum.CARD,PROVIDER,request,keys.getUserId());
                })
                .switchIfEmpty(Mono.defer(() -> {
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
    public Mono<Response<?>> payWithTransfer(String key, String merchantId, TransferPaymentRequestDTO request) {

        return keysRepository.findByUserIdAndSecretKey(merchantId,key)
                .flatMap(keys -> {
                    if (keys.getKeyUsage().equals(KeyUsage.TEST))
                        return sandboxEnvironment.getResponse(CardPaymentResponseDTO.builder().build());
                    else
                        return paymentServiceRouter.routePayment(PaymentTypeEnum.TRANSFER,PROVIDER,request, keys.getUserId());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("keys not found for user");
                    return Mono.just(Response.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(FAILED)
                            .errors(List.of("keys not found for merchant"))
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
    public Mono<Response<?>> payWithUSSD(String key, String merchantId, USSDPaymentRequestDTO request) {
        return keysRepository.findByUserIdAndSecretKey(merchantId,key)
                .flatMap(keys -> {
                    if (keys.getKeyUsage().equals(KeyUsage.TEST))
                        return sandboxEnvironment.getResponse(CardPaymentResponseDTO.builder().build());
                    else
                        return paymentServiceRouter.routePayment(PaymentTypeEnum.USSD,PROVIDER,request, keys.getUserId());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("keys not found for user");
                    return Mono.just(Response.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(FAILED)
                            .errors(List.of("keys not found for merchant"))
                            .build());
                }));    }

    @Override
    public Mono<Response<?>> payWithPaymentLink(String key, String merchantId, PaymentLinkRequestDTO request) {

        return keysRepository.findByUserIdAndSecretKey(merchantId,key)
                .flatMap(keys -> {
                    if (keys.getKeyUsage().equals(KeyUsage.TEST))
                        return sandboxEnvironment.getResponse(CardPaymentResponseDTO.builder().build());
                    else
                        return paymentServiceRouter.routePayment(PaymentTypeEnum.PAYMENT_LINK,PROVIDER,request, keys.getUserId());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("keys not found for user");
                    return Mono.just(Response.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(FAILED)
                            .errors(List.of("keys not found for merchant"))
                            .build());
                }));    }

    @Override
    public Mono<Response<Object>> webhook(Map<String, Object> request) {
//        Transaction transaction = new Transaction();
//        String event = "";
//        if (request.containsKey("event"))
//            event = (String) request.get("event");
//
//        if (event.contains("success"))
//            transaction.setTransactionStatus(Status.SUCCESSFUL);
//        else if (event.contains("failed")) {
//            transaction.setTransactionStatus(Status.FAILED);
//        }
//        else if (event.contains("failed")) {
//            transaction.setTransactionStatus(Status.FAILED);
//        }
        return null;
    }
}
