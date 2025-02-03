package com.example.gateway.api.paymentlink.interfaces;

import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.paymentlink.dto.PaymentLinkDTO;
import com.example.gateway.integrations.paymentlink.utils.ResponseDTO;
import reactor.core.publisher.Mono;

public interface IPaymentLinkService {
    Mono<Response<Object>> generatePaymentLink(String merchantId, String secret, PaymentLinkDTO request, String userId);
    Mono<Response<Object>> viewLinkStatus(String merchantId, String secret, String linkId, String userId);
    Mono<Response<Object>> fetchAllLinksForMerchant(String merchantId, String secret, String userId);

    Mono<ResponseDTO<Object>> getPaymentLinkDetails(String linkId, String reference);
}
