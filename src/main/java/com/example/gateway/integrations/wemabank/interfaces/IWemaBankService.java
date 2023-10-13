package com.example.gateway.integrations.wemabank.interfaces;

import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.wemabank.dtos.*;
import reactor.core.publisher.Mono;

public interface IWemaBankService {
    Mono<WemaTransactionNotificationResponse> receiveTransactionNotification(WemaTransactionDTO request);
    Mono<WemaAccountLookupResponse> accountLookup(AccountLookupRequest request);
    Mono<WemaTransactionDTO> tranasctionQuery(WemaTransactionQueryRequest request);
}
