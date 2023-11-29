package com.example.gateway.integrations.wemabank.interfaces;

import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import com.example.gateway.integrations.wemabank.dtos.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IWemaBankService {
    Mono<WemaTransactionNotificationResponse> receiveTransactionNotification(WemaTransactionDTO request);
    Mono<WemaAccountLookupResponse> accountLookup(AccountLookupRequest request);
    Mono<WemaTransactionDTO> tranasctionQuery(WemaTransactionQueryRequest request);

    Mono<Response<Object>> payWithTransfer(String merchantId, PayWithTransferDTO transferPaymentRequestDTO);

    Mono<Response<String>>getVestraPoolAccountBalance(String accountNumber);
    Mono<Response<List<Object>>>getStatement(GetStatementDTO request);

    Mono<Response<Object>>fundsTransfer();
    Mono<Response<Object>>bankNameEnquiry();
}
