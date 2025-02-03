package com.example.gateway.api.card.interfaces;

import com.example.gateway.api.card.dtos.CardPaymentRequestDTO;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestAVS;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestOTP;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestPhone;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestPin;
import reactor.core.publisher.Mono;

public interface ICardService {
    Mono<Response<?>> payWithCard(CardPaymentRequestDTO cardPaymentRequestDTO, String merchantId, String userId);
    Mono<Response<Object>> authorizeCard(AuthorizeCardRequestPin request, String merchantId, String userId);
    Mono<Response<Object>> authorizeCardOtp(AuthorizeCardRequestOTP request, String merchantId, String userId);
    Mono<Response<Object>> authorizeCardAvs(AuthorizeCardRequestAVS request, String merchantId, String userId);
    Mono<Response<Object>> authorizeCardPhone(AuthorizeCardRequestPhone request, String merchantId, String userId);

    String getName();

}
