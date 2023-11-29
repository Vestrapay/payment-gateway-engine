package com.example.gateway.integrations;

import com.example.gateway.transactions.models.Transaction;
import reactor.core.publisher.Mono;

public interface ITSQService {
    Mono<Transaction> doTSQ(Transaction transaction);
}
