package com.example.gateway.commons.services;

import com.example.gateway.commons.dto.card.CardPaymentRequestDTO;
import com.example.gateway.commons.dto.transfer.TransferPaymentRequestDTO;
import com.example.gateway.commons.repository.RoutingRuleRepository;
import com.example.gateway.commons.utils.ObjectMapperUtil;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.KoraPayWithCardRequest;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import com.example.gateway.integrations.kora.interfaces.IKoraService;
import com.example.gateway.integrations.mastercard.dto.PaymentByCardRequestVO;
import com.example.gateway.integrations.mastercard.interfaces.IMasterCardService;
import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
import com.example.gateway.transactions.enums.PaymentTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceRouter {
    private final IMasterCardService masterCardService;
    private final IKoraService koraService;
    private final IWemaBankService wemaBankService;
    private final RoutingRuleRepository routingRuleRepository;
    @Value("${default.provider}")
    String defaultProvider;

    public Mono<Response<?>>routePayment(PaymentTypeEnum paymentType, Object request, String merchantId){
        switch (paymentType){
            case CARD -> {
                CardPaymentRequestDTO cardPaymentRequestDTO = (CardPaymentRequestDTO) request;
                return routingRuleRepository.findByPaymentMethod(PaymentTypeEnum.CARD.name())
                        .flatMap(routingRule -> {
                            if (routingRule.getProvider().equalsIgnoreCase("MASTERCARD")){
                                PaymentByCardRequestVO masterCardDTO = ObjectMapperUtil.toMasterCardDTO(cardPaymentRequestDTO, merchantId);
                                return masterCardService.makePayment(masterCardDTO)
                                        .flatMap(masterCardMakePaymentResponseResponse -> {
                                            return Mono.just(Response.builder()
                                                    .data(masterCardMakePaymentResponseResponse.getData())
                                                    .status(masterCardMakePaymentResponseResponse.getStatus())
                                                    .errors(masterCardMakePaymentResponseResponse.getErrors())
                                                    .message(masterCardMakePaymentResponseResponse.getMessage())
                                                    .statusCode(masterCardMakePaymentResponseResponse.getStatusCode())
                                                    .build());
                                        });
                            } else if (routingRule.getProvider().equalsIgnoreCase("KORAPAY")) {
                                KoraPayWithCardRequest koraPayCardDTO = ObjectMapperUtil.toKoraPayCardDTO(cardPaymentRequestDTO, merchantId);
                                return koraService.payWithCard(koraPayCardDTO,merchantId);
                            }
                            log.error("no implementation for provider {}",routingRule.getProvider());
                            return Mono.just(Response.builder()
                                    .errors(List.of("no implementation for provider {}"+routingRule.getProvider()))
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .message("Failed")
                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .build());
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
            case TRANSFER -> {
                TransferPaymentRequestDTO transferPaymentRequestDTO = (TransferPaymentRequestDTO) request;
                PayWithTransferDTO transferRequestDTO = ObjectMapperUtil.toKoraPayTransferDTO(transferPaymentRequestDTO, merchantId);
                return routingRuleRepository.findByPaymentMethod(PaymentTypeEnum.TRANSFER.name())
                        .flatMap(routingRule -> {
                            if (routingRule.getProvider().equalsIgnoreCase("KORAPAY")){
                                return koraService.payWithTransfer(transferRequestDTO,merchantId).flatMap(objectResponse -> {
                                    return Mono.just(Response.builder()
                                            .data(objectResponse.getData())
                                            .statusCode(objectResponse.getStatusCode())
                                            .status(objectResponse.getStatus())
                                            .message(objectResponse.getMessage())
                                            .errors(objectResponse.getErrors())
                                            .build());
                                });
                            } else if (routingRule.getProvider().equalsIgnoreCase("WEMA")) {
                                return wemaBankService.payWithTransfer(merchantId,transferRequestDTO)
                                        .flatMap(wemaAccountsResponse -> {
                                            return Mono.just(Response.builder()
                                                    .data(wemaAccountsResponse.getData())
                                                    .errors(wemaAccountsResponse.getErrors())
                                                    .message(wemaAccountsResponse.getMessage())
                                                    .status(wemaAccountsResponse.getStatus())
                                                    .statusCode(wemaAccountsResponse.getStatusCode())
                                                    .build());
                                        });
                            }
                            else {
                                log.error("no implementation for provider {}",routingRule.getProvider());
                                return Mono.just(Response.builder()
                                        .errors(List.of("no implementation for provider {}"+routingRule.getProvider()))
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .message("Failed")
                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .build());
                            }


                        });


            }

            default -> {
                return Mono.empty();
            }
        }
    }

}
