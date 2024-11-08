package com.example.gateway.commons.utils;

import com.example.gateway.api.card.dtos.CardPaymentRequestDTO;
import com.example.gateway.api.transfer.dtos.TransferPaymentRequestDTO;
import com.example.gateway.integrations.kora.dtos.card.*;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import com.example.gateway.integrations.mastercard.dto.CardDetails;
import com.example.gateway.integrations.mastercard.dto.Currency;
import com.example.gateway.integrations.mastercard.dto.PaymentByCardRequestVO;

import java.util.Objects;
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
                .card(Card2.builder()
                        .name(request.getCard().getName())
                        .number(request.getCard().getNumber())
                        .cvv(request.getCard().getCvv())
                        .pin(request.getCard().getPin())
                        .expiryMonth(request.getCard().getExpiryMonth())
                        .expiryYear(request.getCard().getExpiryYear())
                        .build())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .metadata(MetaData2.builder()
                        .internalRef(UUID.randomUUID().toString())
                        .age(15)
                        .fixed(true)
                        .build())
                .build();
    }

    public static PayWithTransferDTO toKoraPayTransferDTO(TransferPaymentRequestDTO request){
        return PayWithTransferDTO.builder()
                .notificationUrl("")
                .currency(request.getCurrency())
                .reference(request.getTransactionReference().isEmpty()?UUID.randomUUID().toString(): request.getTransactionReference())
                .customer(request.getCustomer())
                .amount(request.getAmount())
                .metaData(Objects.isNull(request.getMetaData())?null:request.getMetaData().toString())
                .build();
    }
}
