package com.example.gateway.integrations.wemabank.services;

import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.utils.AESEncryptionUtils;
import com.example.gateway.commons.utils.HttpUtil;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.PaymentTypeInterface;
import com.example.gateway.integrations.wemabank.accounts.models.WemaAccounts;
import com.example.gateway.integrations.wemabank.accounts.repository.WemaAccountRepository;
import com.example.gateway.integrations.wemabank.dtos.*;
import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
import com.example.gateway.integrations.wemabank.utils.WemaTokenUtils;
import com.example.gateway.transactions.models.Transaction;
import com.example.gateway.transactions.reporitory.TransactionRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Qualifier("WemaBankService")
public class WemaBankService implements IWemaBankService, PaymentTypeInterface {
    private final TransactionRepository transactionRepository;
    private final WemaAccountRepository wemaAccountRepository;
    private final HttpUtil httpUtil;
    private final WemaTokenUtils tokenUtils;
    private final Gson gson;

    @Value("${wema.base.url}")
    private String baseUrl;
    @Value("${encryption.key}")
    static String secretKey;
    @Value("${encryption.iv}")
    static String initVector;
    @Override
    public Mono<WemaTransactionNotificationResponse> receiveTransactionNotification(WemaTransactionDTO request) {
        Transaction transaction = Transaction.builder().build();
        return transactionRepository.save(transaction)
                .flatMap(transaction1 -> {
                    log.info("transaction notification from wemabank saved successfully");
                    return Mono.just(WemaTransactionNotificationResponse.builder()
                                    .transactionReference(transaction1.getUuid())
                                    .status("OO")
                                    .status("Okay")
                            .build());
                }).onErrorResume(throwable -> {
                    log.error("error saving transaction notification from wemabank error is {}",throwable.getLocalizedMessage());
                    return Mono.just(WemaTransactionNotificationResponse.builder()
                            .status("91")
                            .status("Not-Okay")
                            .build());
                });
    }

    @Override
    public Mono<WemaAccountLookupResponse> accountLookup(AccountLookupRequest request) {
        return wemaAccountRepository.findByAccountNumber(request.getAccountNumber())
                .flatMap(settlement -> {
                    log.info("aaccount gotten. {}",settlement.toString());
                    return Mono.just(WemaAccountLookupResponse.builder()
                                    .accountName(settlement.getAccountName())
                                    .status("00")
                                    .statusDescription("Okay")
                            .build());
                }).switchIfEmpty(Mono.defer(() -> {
                    log.error("account not found for {}", request);
                    return Mono.just(WemaAccountLookupResponse.builder()
                            .status("07")
                            .statusDescription("Invalid Account")
                            .build());
                }));
    }

    @Override
    //for this the transaction amount needs to be provided
    public Mono<WemaTransactionDTO> tranasctionQuery(WemaTransactionQueryRequest request) {

        return null;
    }

    @Override
    public Mono<Response<WemaAccounts>> payWithTransfer(String merchantId) {
        return wemaAccountRepository.findByMerchantId(merchantId)
                .flatMap(wemaAccounts -> {
                    log.info("merchant {} wema account gotten",merchantId);
                    // TODO: 23/10/2023 generate wema account from the virtual account API
                    return Mono.just(Response.<WemaAccounts>builder()
                                    .message("SUCCESSFUL")
                                    .data(wemaAccounts)
                                    .statusCode(HttpStatus.OK.value())
                                    .status(HttpStatus.OK)
                            .build());

                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("wema account not found for merchant");
                    return Mono.just(Response.<WemaAccounts>builder()
                                    .statusCode(HttpStatus.NOT_FOUND.value())
                                    .status(HttpStatus.NOT_FOUND)
                                    .errors(List.of("Account not found for merchant"))
                            .build());
                }))
                .doOnError(throwable -> {
                    log.error("error fetching wema account for merchant {}",merchantId);
                    throw new CustomException(Response.builder()
                            .errors(List.of(throwable.getLocalizedMessage(),"error fetching account"))
                            .message("FAILED")
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    @Override
    public Mono<Response<String>> getVestraPoolAccountBalance(String accountNumber) {
        log.info("fetching vestrapay pool accounts");
        String encryptedAccountNumber = AESEncryptionUtils.encrypt(accountNumber,initVector,secretKey);
        return tokenUtils.getToken()
                        .flatMap(mapResponse -> {
                            if (Objects.isNull(mapResponse.getData())){
                                return Mono.just(Response.<String>builder()
                                                .message("FAILED")
                                                .status(HttpStatus.UNAUTHORIZED)
                                                .statusCode(HttpStatus.UNAUTHORIZED.value())
                                                .errors(List.of("Token not gotten"))
                                        .build());
                            }
                            String token = mapResponse.getData().getToken();
                            Map<String,String> header= Map.of("Authorization","Bearer "+token);
                            return httpUtil.get(baseUrl.concat("/api/GetTransactionStatus?"+encryptedAccountNumber),header,60)
                                    .flatMap(clientResponse -> {
                                        if (!clientResponse.statusCode().is2xxSuccessful()){
                                            return Mono.just(Response.<String>builder()
                                                    .statusCode(HttpStatus.EXPECTATION_FAILED.value())
                                                    .status(HttpStatus.EXPECTATION_FAILED)
                                                    .message("Failed")
                                                    .build());
                                        }
                                        else
                                            return clientResponse.bodyToMono(String.class)
                                                    .flatMap(response -> {
                                                        return Mono.just(Response.<String>builder()
                                                                .data(AESEncryptionUtils.decrypt(response,initVector,secretKey))
                                                                .statusCode(HttpStatus.OK.value())
                                                                .status(HttpStatus.OK)
                                                                .message("Successful")
                                                                .build());
                                                    });

                                    });

                        });
    }

    @Override
    public Mono<Response<List<Object>>> getStatement(GetStatementDTO request) {
        return null;
    }


    @Override
    public String getPaymentType() {
        return "TRANSFER";
    }
}
