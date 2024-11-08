package com.example.gateway.api.card.interfaces;

import com.example.gateway.api.card.dtos.CardPaymentRequestDTO;
import com.example.gateway.commons.utils.Response;
import reactor.core.publisher.Mono;

public interface ICardService {
    Mono<Response<?>> payWithCard(String authorization, String merchantId, CardPaymentRequestDTO request, String customerId);
    Mono<Response<Object>> doTSQ(String secret, String merchantId, String reference,String customerId);
}
