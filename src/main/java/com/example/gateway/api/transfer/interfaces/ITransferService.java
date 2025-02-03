package com.example.gateway.api.transfer.interfaces;

import com.example.gateway.api.transfer.dtos.TransferPaymentRequestDTO;
import com.example.gateway.commons.utils.Response;
import reactor.core.publisher.Mono;

public interface ITransferService {
    Mono<Response<Object>> payWithTransfer(TransferPaymentRequestDTO request, String merchantId, String userId);
    String getName();

}
