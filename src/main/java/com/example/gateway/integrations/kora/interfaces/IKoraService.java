package com.example.gateway.integrations.kora.interfaces;

import com.example.gateway.commons.transactions.models.Transaction;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.*;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import reactor.core.publisher.Mono;

public interface IKoraService {
    Mono<Response<?>> payWithCard(KoraPayWithCardRequest request, String merchantId,String userId);
    Mono<Response<?>>chargeCard(String request,String merchantId,String reference);
    Mono<Response<Object>> authorizeCard(AuthorizeCardRequestPin request, String merchantId, String userId);

    Mono<Response<Object>> authorizeCardOtp(AuthorizeCardRequestOTP request, String merchantId, String userId);
    Mono<Response<Object>> authorizeCardAvs(AuthorizeCardRequestAVS request, String merchantId, String userId);
    Mono<Response<Object>> authorizeCardPhone(AuthorizeCardRequestPhone request, String merchantId, String userId);
    Mono<Response<Object>> verifyTransaction(String reference, String merchantId,String userId);
    Mono<Transaction> korapayTransactionStatusCheck(Transaction transaction);

    Mono<Response<Object>> payWithTransfer(PayWithTransferDTO request,String merchantId,String userId);
}
