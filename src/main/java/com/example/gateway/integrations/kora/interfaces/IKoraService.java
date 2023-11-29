package com.example.gateway.integrations.kora.interfaces;

import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.*;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import reactor.core.publisher.Mono;

public interface IKoraService {
    Mono<Response<?>> payWithCard(KoraPayWithCardRequest request, String merchantId);
    Mono<Response<?>>chargeCard(String request,String merchantId,String reference);

    Mono<Response<String>> decrypt(EncryptDecryptRequest request);

    Mono<Response<String>> encrypt(EncryptDecryptRequest request) throws Exception;

    Mono<Response<Object>> authorizeCard(AuthorizeCardRequestPin request, String merchantId);

    Mono<Response<Object>> authorizeCardOtp(AuthorizeCardRequestOTP request, String merchantId);
    Mono<Response<Object>> authorizeCardAvs(AuthorizeCardRequestAVS request, String merchantId);
    Mono<Response<Object>> authorizeCardPhone(AuthorizeCardRequestPhone request, String merchantId);
    Mono<Response<Object>> verifyTransaction(String reference, String merchantId);

    Mono<Response<Object>> payWithTransfer(PayWithTransferDTO request,String merchantId);
}
