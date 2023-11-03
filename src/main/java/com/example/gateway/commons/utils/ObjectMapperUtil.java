package com.example.gateway.commons.utils;

import com.example.gateway.commons.dto.card.CardPaymentRequestDTO;
import com.example.gateway.commons.dto.transfer.TransferPaymentRequestDTO;
import com.example.gateway.integrations.kora.dtos.card.Card;
import com.example.gateway.integrations.kora.dtos.card.KoraPayWithCardRequest;
import com.example.gateway.integrations.kora.dtos.card.MetaData;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import com.example.gateway.integrations.mastercard.dto.CardDetails;
import com.example.gateway.integrations.mastercard.dto.Currency;
import com.example.gateway.integrations.mastercard.dto.PaymentByCardRequestVO;

import java.util.UUID;

public class ObjectMapperUtil {
    public static PaymentByCardRequestVO toMasterCardDTO(CardPaymentRequestDTO request,String merchantId){
        return PaymentByCardRequestVO.builder()
                .amount(request.getAmount().toString())
                .reference(request.getTransactionReference().isBlank()?UUID.randomUUID().toString(): request.getTransactionReference())
                .callBackUrl("")
                .currency(Currency.valueOf(request.getCurrency()))
                .merchantId(merchantId)
                .customerId(request.getCustomerDetails().getEmail())
                .cardDetails(CardDetails.builder()
                        .authDataVersion("v7")
                        .cvv2(request.getCard().getCvv())
                        .expiryDate(request.getCard().getExpiryMonth()+request.getCard().getExpiryYear())
                        .pan(request.getCard().getNumber())
                        .pin(request.getCard().getPin())
                        .build())
                .build();
    }

    public static KoraPayWithCardRequest toKoraPayCardDTO(CardPaymentRequestDTO request, String merchantId){
        return KoraPayWithCardRequest.builder()
                .redirectUrl("")
                .reference(request.getTransactionReference().isEmpty()?UUID.randomUUID().toString(): request.getTransactionReference())
                .customer(request.getCustomerDetails())
                .card(Card.builder()
                        .name(request.getCard().getName())
                        .number(request.getCard().getNumber())
                        .cvv(request.getCard().getCvv())
                        .pin(request.getCard().getPin())
                        .expiryMonth(request.getCard().getExpiryMonth())
                        .expiryYear(request.getCard().getExpiryYear())
                        .build())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .metadata(MetaData.builder()
                        .build())
                .build();
    }

    public static PayWithTransferDTO toKoraPayTransferDTO(TransferPaymentRequestDTO request, String merchantId){
        return PayWithTransferDTO.builder()
                .notificationUrl("")
                .currency(request.getCurrency())
                .reference(request.getTransactionReference().isEmpty()?UUID.randomUUID().toString(): request.getTransactionReference())
                .customer(request.getCustomer())
                .amount(request.getAmount())
                .build();
    }
}
