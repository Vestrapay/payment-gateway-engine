package com.example.gateway.integrations.wemabank.interfaces;

import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.wemabank.accounts.models.WemaAccounts;
import com.example.gateway.integrations.wemabank.dtos.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IWemaBankService {
    Mono<WemaTransactionNotificationResponse> receiveTransactionNotification(WemaTransactionDTO request);
    Mono<WemaAccountLookupResponse> accountLookup(AccountLookupRequest request);
    Mono<WemaTransactionDTO> tranasctionQuery(WemaTransactionQueryRequest request);

    Mono<Response<WemaAccounts>> payWithTransfer(String merchantId);

    Mono<Response<String>>getVestraPoolAccountBalance(String accountNumber);
    Mono<Response<List<Object>>>getStatement(GetStatementDTO request);
}
