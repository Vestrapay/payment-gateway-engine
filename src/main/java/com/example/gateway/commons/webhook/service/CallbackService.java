package com.example.gateway.commons.webhook.service;

import com.example.gateway.commons.keys.enums.KeyUsage;
import com.example.gateway.commons.keys.repository.KeysRepository;
import com.example.gateway.commons.utils.HttpUtil;
import com.example.gateway.commons.utils.WebhookUtils;
import com.example.gateway.commons.transactions.models.Transaction;
import com.example.gateway.commons.webhook.repository.WebhookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.example.gateway.commons.utils.WebhookUtils.generateHMAC;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackService {
    private final WebhookRepository webhookRepository;
    private final KeysRepository keysRepository;
    private final HttpUtil httpUtil;
    private final Gson gson;

    @Value("${server.environment}")
    String environment;
    public void sendNotification(Transaction transaction){
        log.info("about sending notification for transaction {}",transaction.getTransactionReference());
        CompletableFuture.supplyAsync(() -> webhookRepository.findByMerchantId(transaction.getMerchantId())
                .flatMap(webhook ->{
                    return keysRepository.findByUserIdAndKeyUsage(transaction.getMerchantId(),KeyUsage.valueOf(environment).name())
                            .flatMap(keys -> {
                                String payload = "";
                                String jsonPayload = "";
                                String messageHash = "";
                                //check webhook has secretHash then perform AES encryption on the payload, else send plain
                                String secretHash = webhook.getSecretHash();
                                Map<String,Object> responseObject = new HashMap<>();

                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    mapper.registerModule(new JavaTimeModule());

                                    jsonPayload = mapper.writeValueAsString(transaction);
                                    messageHash = generateHMAC(jsonPayload,keys.getSecretKey());

                                    if (Strings.isNotEmpty(secretHash)){
                                        payload = WebhookUtils.encrypt(jsonPayload,secretHash);
                                        responseObject.put("encryption",true);
                                        responseObject.put("algorithm","AES");
                                    }
                                    else
                                    {
                                        payload = jsonPayload;
                                    }

                                }catch (Exception e){
                                    log.warn("exception caught sending webhook notification. exception is {} for transaction {}",e.getMessage(),transaction );
                                    payload = jsonPayload;
                                    responseObject.put("error",e.getMessage());
                                }

                                responseObject.put("payload",payload);

                                return httpUtil.post(webhook.getUrl(),responseObject,Map.of("x-vestrapay-signature",messageHash),30)
                                        .flatMap(clientResponse -> {
                                            if (clientResponse.statusCode().is2xxSuccessful()){
                                                log.info("Transaction {} successfully notified",transaction.getTransactionReference());
                                            }
                                            else
                                                log.info("Transaction {} not successfully notified",transaction.getTransactionReference());
                                            return Mono.just(transaction);

                                        })
                                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(30)));
                                //todo implement saving failed notifications to a new table for scheduler to pick up (futuristic)

                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.error("keys not found for merchant {} for transaction reference. kindly investigate {}",transaction.getMerchantId(),transaction.getTransactionReference());
                                return Mono.empty();
                            }));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("no webhook configured for merchant {}",transaction.getMerchantId());
                    return Mono.empty();
                }))
                .subscribe());

    }

}
