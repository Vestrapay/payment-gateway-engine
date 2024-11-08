package com.example.gateway.commons.settlements.service;

import com.example.gateway.commons.settlements.models.Settlement;
import com.example.gateway.commons.transactions.models.Transaction;
import reactor.core.publisher.Mono;

public interface ISettlementService {
    Mono<Transaction> pushSettlement(Transaction transaction, Settlement settlementAccount);

    String getProvider();
}
