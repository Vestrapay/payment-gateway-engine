package com.example.gateway.commons.jobs;

import com.example.gateway.commons.notificatioin.NotificationService;
import com.example.gateway.commons.settlements.factory.SettlementFactory;
import com.example.gateway.commons.settlements.models.Settlement;
import com.example.gateway.commons.settlements.repository.SettlementRepository;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class SettlementJob {
    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementFactory settlementFactory;
    private final NotificationService notificationService;
    @Value("${spring.profiles.active}")
    String profile;
    private boolean running = false;
    @Scheduled(cron = "0 0 0 * * *")
    public void doSettlement(){
        log.info("starting auto settlement");
        if (profile.equalsIgnoreCase("cron")){
            if (!running){
                running = true;
                LocalDateTime now = LocalDateTime.now();
                transactionRepository.findByTransactionStatusAndSettlementStatusAndCreatedAtBetween(Status.SUCCESSFUL,Status.PENDING, now.toLocalDate().minusDays(10).atStartOfDay(),now.toLocalDate().atStartOfDay())
                        .collectList()
                        .flatMap(transactions -> {
                            transactions.parallelStream()
                                    .forEach(transaction -> {
                                        String merchantId = transaction.getMerchantId();
                                        settlementRepository.findByMerchantIdAndPrimaryAccount(merchantId,true)
                                                .collectList()
                                                .doOnNext(settlements -> {
                                                    if (settlements.isEmpty()){
                                                        transaction.setActivityStatus("no settlement account found for merchant");
                                                        transactionRepository.save(transaction).subscribe();
                                                        throw new RuntimeException("no settlement account found for merchant "+merchantId);
                                                    }
                                                    Settlement settlementAccount = settlements.stream().filter(Settlement::isPrimaryAccount)
                                                            .findFirst().orElse(settlements.get(0));

                                                    //push transaction to Provider for settlement
                                                    settlementFactory.getSettlementService().pushSettlement(transaction,settlementAccount)
                                                            .doOnNext(transaction1 -> {
                                                                if (transaction.getSettlementStatus().equals(Status.SUCCESSFUL))
                                                                    notificationService.postSettlementNotification(transaction1);
                                                                transactionRepository.save(transaction1).subscribe();
                                                            })
                                                            .doOnError(throwable -> log.error(throwable.getMessage()))
                                                            .subscribe();
                                                })
                                                .switchIfEmpty(Mono.defer(() -> {
                                                    log.error("no settlement account configured for merchant {}",transaction.getMerchantId());
                                                    notificationService.postSettlementError("no settlement account configured for merchant ",transaction.getMerchantId());
                                                    return Mono.just(null);
                                                }))
                                                .doOnError(throwable -> {
                                                    log.error("error is {}",throwable.getMessage());
                                                })
                                                .subscribe();

                                    });
                            return Mono.empty();
                        }).subscribe();
                running = false;
            }


        }
        else
            log.info("profile not active for cron jobs");

    }


}
