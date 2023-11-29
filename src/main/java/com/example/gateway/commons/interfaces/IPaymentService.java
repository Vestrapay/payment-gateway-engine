package com.example.gateway.commons.interfaces;

import com.example.gateway.commons.dto.card.CardPaymentRequestDTO;
import com.example.gateway.commons.dto.paymentlink.PaymentLinkRequestDTO;
import com.example.gateway.commons.dto.transfer.TransferPaymentRequestDTO;
import com.example.gateway.commons.dto.ussd.USSDPaymentRequestDTO;
import com.example.gateway.commons.utils.Response;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

public interface IPaymentService {
    Mono<Response<?>> payWithCard(String authorization, String merchantId, CardPaymentRequestDTO request);
    Mono<Response<?>> payWithTransfer(String authorization, String merchantId, TransferPaymentRequestDTO request);
    Mono<Response<Object>> payWithUSSD(String authorization, String merchantId, USSDPaymentRequestDTO request);
    Mono<Response<?>> payWithPaymentLink(String authorization, String merchantId, PaymentLinkRequestDTO request);

    Mono<Response<Object>> webhook(@Valid Object request, String provider);

    Mono<Response<Object>> doTSQ(String secret, String merchantId, String reference);
}
