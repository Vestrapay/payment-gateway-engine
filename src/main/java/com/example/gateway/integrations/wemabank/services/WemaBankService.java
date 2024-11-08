package com.example.gateway.integrations.wemabank.services;

import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.utils.AESEncryptionUtils;
import com.example.gateway.commons.utils.HttpUtil;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.PaymentTypeInterface;
import com.example.gateway.integrations.kora.dtos.transfer.BankAccount;
import com.example.gateway.integrations.kora.dtos.transfer.Data;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferResponseDTO;
import com.example.gateway.integrations.wemabank.accounts.repository.WemaAccountRepository;
import com.example.gateway.integrations.wemabank.dtos.*;
import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
import com.example.gateway.integrations.wemabank.utils.WemaTokenUtils;
import com.example.gateway.commons.transactions.enums.PaymentTypeEnum;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.models.Transaction;
import com.example.gateway.commons.transactions.reporitory.TransactionRepository;
import com.example.gateway.commons.transactions.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Qualifier("WemaBankService")
public class WemaBankService implements IWemaBankService, PaymentTypeInterface {
    private final TransactionRepository transactionRepository;
    private final WemaAccountRepository wemaAccountRepository;
    private final HttpUtil httpUtil;
    private final WemaTokenUtils tokenUtils;
    private final TransactionService transactionService;

    @Value("${wema.base.url}")
    private String baseUrl;
    @Value("${encryption.key}")
    static String secretKey;
    @Value("${encryption.iv}")
    static String initVector;
    private static final String PROVIDER_NAME = "WEMA";
    @Override
    public Mono<WemaTransactionNotificationResponse> receiveTransactionNotification(WemaTransactionDTO request) {
        return transactionRepository.findByTransactionReferenceAndAmount(request.getPaymentReference(),new BigDecimal(request.getAmount()))
                .flatMap(transaction -> {
                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                    transaction.setNarration(request.getNarration());
                    return transactionService.updateTransaction(transaction)
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
                }).switchIfEmpty(Mono.defer(() -> {
                    log.error("transaction with reference {} not found for wema notification, payload is {}",request.getPaymentReference(),request);
                    return Mono.empty();
                }));

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
    public Mono<Response<Object>> payWithTransfer(String merchantId, PayWithTransferDTO request,String userId) {
        Transaction tranLog = Transaction.builder()
                .transactionReference(request.getReference())
                .amount(request.getAmount())
                .transactionStatus(Status.ONGOING)
                .pan("WEMA_TRANSFER")
                .cardScheme("TRANSFER")
                .paymentType(PaymentTypeEnum.TRANSFER)
                .userId(userId)
                .vestraPayReference(UUID.randomUUID().toString())
                .merchantId(merchantId)
                .providerName(PROVIDER_NAME)
                .activityStatus("transaction initiated at "+new Date())
                .narration(request.getCustomer().getEmail())
                .userId(request.getCustomer().getEmail())
                .build();

        return transactionService.saveTransaction(tranLog,PaymentTypeEnum.TRANSFER,merchantId)
                .flatMap(transaction-> wemaAccountRepository.findByMerchantId(merchantId)
                        .flatMap(wemaAccounts -> {
                            log.info("merchant {} wema account gotten",merchantId);
                            PayWithTransferResponseDTO responseDTO = PayWithTransferResponseDTO.builder()
                                    .status(true)
                                    .message("Bank transfer initiated successfully")
                                    .data(Data.builder()
                                            .amount(request.getAmount())
                                            .fee(BigDecimal.ZERO)
                                            .status("Processing")
                                            .currency(request.getCurrency())
                                            .amountExpected(request.getAmount())
                                            .customer(request.getCustomer())
                                            .merchantBearsCost(true)
                                            .vat(BigDecimal.valueOf(3.75))
                                            .paymentReference(tranLog.getVestraPayReference())
                                            .reference(tranLog.getVestraPayReference())
                                            .narration("Payment on VESTRAPAY NIGERIA LIMITED")
                                            .bankAccount(BankAccount.builder()
                                                    .bankCode("945")
                                                    .bankName("WEMA BANK")
                                                    .accountName(wemaAccounts.getAccountName())
                                                    .accountNumber(wemaAccounts.getAccountNumber())
                                                    .build())
                                            .build())
                                    .build();

                            return Mono.just(Response.builder()
                                    .message("SUCCESSFUL")
                                    .data(responseDTO)
                                    .statusCode(HttpStatus.OK.value())
                                    .status(HttpStatus.OK)
                                    .build());

                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            log.error("wema account not found for merchant");
                            return Mono.just(Response.builder()
                                    .statusCode(HttpStatus.NOT_FOUND.value())
                                    .status(HttpStatus.NOT_FOUND)
                                    .errors(List.of("Account not found for merchant"))
                                    .build());
                        }))
                        .doOnError(throwable -> {
                            log.error("error fetching wema account for merchant {}",merchantId);
                            throw new CustomException(throwable);
                        }))
                .doOnError(throwable -> {
                    log.error("error saving transaction request for pay with transfer with DTO {}", request);
                    throw new CustomException(Response.builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Failed")
                            .errors(List.of(throwable.getMessage()))
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
                                                    .flatMap(response -> Mono.just(Response.<String>builder()
                                                            .data(AESEncryptionUtils.decrypt(response,initVector,secretKey))
                                                            .statusCode(HttpStatus.OK.value())
                                                            .status(HttpStatus.OK)
                                                            .message("Successful")
                                                            .build()));

                                    });

                        });
    }

    @Override
    public Mono<Response<List<Object>>> getStatement(GetStatementDTO request) {
        return null;
    }

    @Override
    public Mono<Response<Object>> fundsTransfer() {
        return null;
    }

    @Override
    public Mono<Response<Object>> bankNameEnquiry() {
        return null;
    }


    @Override
    public String getPaymentType() {
        return "TRANSFER";
    }
}
