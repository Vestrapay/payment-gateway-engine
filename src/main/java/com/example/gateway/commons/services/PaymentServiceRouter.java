package com.example.gateway.commons.services;

import com.example.gateway.commons.dto.card.CardPaymentRequestDTO;
import com.example.gateway.commons.dto.transfer.TransferPaymentRequestDTO;
import com.example.gateway.commons.utils.ObjectMapperUtil;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.KoraPayWithCardRequest;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import com.example.gateway.integrations.kora.interfaces.IKoraService;
import com.example.gateway.integrations.mastercard.dto.PaymentByCardRequestVO;
import com.example.gateway.integrations.mastercard.interfaces.IMasterCardService;
import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
import com.example.gateway.transactions.enums.PaymentTypeEnum;
import com.example.gateway.transactions.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceRouter {
    private final IMasterCardService masterCardService;
    private final IKoraService koraService;
    private final IWemaBankService wemaBankService;
    private final TransactionService transactionService;
    public Mono<Response<?>>routePayment(PaymentTypeEnum paymentType, String provider, Object request, String merchantId){
        provider = "MASTERCARD";
        switch (paymentType){
            case CARD -> {
                CardPaymentRequestDTO cardPaymentRequestDTO = (CardPaymentRequestDTO) request;
                if (provider.equalsIgnoreCase("MASTERCARD")){
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
                } else if (provider.equalsIgnoreCase("KORAPAY")) {
                    KoraPayWithCardRequest koraPayCardDTO = ObjectMapperUtil.toKoraPayCardDTO(cardPaymentRequestDTO, merchantId);
                    return koraService.payWithCard(koraPayCardDTO,merchantId);
                }
            }
            case TRANSFER -> {
                TransferPaymentRequestDTO transferPaymentRequestDTO = (TransferPaymentRequestDTO) request;
                if (provider.equalsIgnoreCase("KORAPAY")){
                    PayWithTransferDTO koraPayTransferDTO = ObjectMapperUtil.toKoraPayTransferDTO(transferPaymentRequestDTO, merchantId);
                    return koraService.payWithTransfer(koraPayTransferDTO,merchantId).flatMap(objectResponse -> {
                        return Mono.just(Response.builder()
                                        .data(objectResponse.getData())
                                        .statusCode(objectResponse.getStatusCode())
                                        .status(objectResponse.getStatus())
                                        .message(objectResponse.getMessage())
                                        .errors(objectResponse.getErrors())
                                .build());
                    });
                } else if (provider.equalsIgnoreCase("WEMA")) {
                    return wemaBankService.payWithTransfer("")
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
            }

            default -> {
                return Mono.empty();
            }
        }
        return Mono.empty();
    }

}
