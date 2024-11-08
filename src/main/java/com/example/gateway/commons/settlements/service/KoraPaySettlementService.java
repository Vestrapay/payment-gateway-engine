package com.example.gateway.commons.settlements.service;

import com.example.gateway.commons.banks.model.Bank;
import com.example.gateway.commons.banks.service.ListBanksService;
import com.example.gateway.commons.charge.service.ChargeService;
import com.example.gateway.commons.merchants.enums.UserType;
import com.example.gateway.commons.merchants.repository.UserRepository;
import com.example.gateway.commons.settlements.dtos.BankAccount;
import com.example.gateway.commons.settlements.dtos.Destination;
import com.example.gateway.commons.settlements.dtos.KoraSettlementRequest;
import com.example.gateway.commons.settlements.dtos.KoraSettlementResponse;
import com.example.gateway.commons.settlements.models.Settlement;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.models.Transaction;
import com.example.gateway.commons.utils.HttpUtil;
import com.example.gateway.integrations.kora.dtos.card.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class KoraPaySettlementService implements ISettlementService {
    private final HttpUtil httpUtil;
    @Value("${kora.disbursement}")
    private String koraSettlementUrl;
    @Value("${kora.secret.key}")
    private String koraSecretKey;
    private final UserRepository userRepository;
    private final ChargeService chargeService;

    @Override
    public Mono<Transaction> pushSettlement(Transaction transaction, Settlement settlementAccount) {
        String bankName = settlementAccount.getBankName();
        List<Bank> data = ListBanksService.getBanks().getData();
        Bank settlementBank = data.stream().filter(bank -> bank.getName().equalsIgnoreCase(bankName))
                .findFirst().orElseThrow(() -> new RuntimeException("Settlement bank not found"));

        return userRepository.findByMerchantIdAndUserType(transaction.getMerchantId(),UserType.MERCHANT)
                .flatMap(user -> {
                    transaction.setSettlementReference(UUID.randomUUID().toString());
                    //do name enquiry on account before saving
                    BigDecimal amount = transaction.getAmount().subtract(Objects.requireNonNullElse(transaction.getFee(),BigDecimal.ZERO));
                    Customer customer = new Customer(user.getFirstName()+" "+user.getLastName(),user.getEmail());
                    Destination destination = new Destination("bank_account",
                            amount.toString(),
                            transaction.getCurrency(),
                            transaction.getNarration(),
                            new BankAccount(settlementBank.getCode(),settlementAccount.getAccountNumber()),
                            customer);
                    KoraSettlementRequest request = KoraSettlementRequest.builder()
                            .reference(transaction.getSettlementReference())
                            .destination(destination)
                            .build();
                    log.info("outgoing settlement request body is {}",request);
                    return httpUtil.post(koraSettlementUrl,request,Map.of("Authorization",String.format("Bearer %s",koraSecretKey)),10000)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(KoraSettlementResponse.class)
                                            .flatMap(response -> {
                                                log.info("settlement response from korapay is {}",response);

                                                if (response.isStatus()){
                                                    //kora returns processing. we need to confirm that processing is successful
                                                    transaction.setTransactionStatus(Status.SUCCESSFUL);
                                                }
                                                else {
                                                    log.error("response from Kora not successful");
                                                    transaction.setSettlementStatus(Status.FAILED);
                                                }
                                                transaction.setActivityStatus(response.getMessage());
                                                return Mono.just(transaction);
                                            });
                                }
                                else {
                                    return clientResponse.bodyToMono(KoraSettlementResponse.class)
                                            .flatMap(map -> {
                                                log.info("failed settlement response from korapay is {}",map);
                                                transaction.setSettlementStatus(Status.FAILED);
                                                transaction.setActivityStatus(map.getMessage());
                                                return Mono.just(transaction);
                                            });
                                }
                            });
                }).switchIfEmpty(Mono.defer(() -> {
                    transaction.setActivityStatus("merchant not found");
                    return Mono.just(transaction);
                }));
    }

    @Override
    public String getProvider() {
        return "KORAPAY";
    }
}
