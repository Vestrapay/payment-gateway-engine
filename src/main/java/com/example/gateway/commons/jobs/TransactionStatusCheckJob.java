package com.example.gateway.commons.jobs;

import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import com.example.gateway.integrations.kora.services.KoraPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionStatusCheckJob {
    private final TransactionRepository transactionRepository;
    private final KoraPayService koraPayService;

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
                                    koraPayService.doTSQ(transaction).subscribe();
                                }
                            });

                            return Mono.empty();
                        }).subscribe();

                running = false;
            }

        }

    }

}
