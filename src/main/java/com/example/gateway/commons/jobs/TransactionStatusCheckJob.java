package com.example.gateway.commons.jobs;

import com.example.gateway.commons.notificatioin.NotificationService;
import com.example.gateway.commons.transactions.models.Transaction;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import com.example.gateway.integrations.kora.interfaces.IKoraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionStatusCheckJob {
    private final TransactionRepository transactionRepository;
    private final IKoraService koraPayService;
    private final NotificationService notificationService;

    @Value("${spring.profiles.active}")
    String profile;
    private boolean running;

    @Scheduled(fixedDelay = 5*1000)
    void doTransactionStatusCheck(){
        if (profile.equalsIgnoreCase("cron")){
            log.info("starting korapay transfer tsq");
            if (!running){
                running = true;
                transactionRepository.getFailedTransactions()
                        .collectList()
                        .flatMap(transactions -> {
                            log.info("transaction size is {}",transactions.size());
                            transactions.forEach(transaction -> {
                                if (transaction.getProviderName().equalsIgnoreCase("KORAPAY")){
                                    koraPayService.korapayTransactionStatusCheck(transaction)
                                            .doOnNext(transaction1 -> {
                                                transactionRepository.save(transaction1).subscribe();
                                            })
                                            .subscribe();
                                }
                            });

                            return Mono.empty();
                        }).subscribe();

                running = false;
            }

        }

    }

}
