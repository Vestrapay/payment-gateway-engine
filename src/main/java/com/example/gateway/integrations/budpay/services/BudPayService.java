package com.example.gateway.integrations.budpay.services;

import com.example.gateway.api.card.dtos.CardPaymentRequestDTO;
import com.example.gateway.api.card.interfaces.ICardService;
import com.example.gateway.commons.banks.model.Bank;
import com.example.gateway.commons.banks.service.ListBanksService;
import com.example.gateway.commons.charge.enums.ChargeCategory;
import com.example.gateway.commons.charge.enums.PaymentMethod;
import com.example.gateway.commons.charge.service.ChargeService;
import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.merchants.enums.UserType;
import com.example.gateway.commons.merchants.repository.UserRepository;
import com.example.gateway.commons.notificatioin.NotificationService;
import com.example.gateway.commons.settlements.dtos.KoraSettlementResponse;
import com.example.gateway.commons.settlements.models.Settlement;
import com.example.gateway.commons.settlements.repository.SettlementRepository;
import com.example.gateway.commons.settlements.service.ISettlementService;
import com.example.gateway.commons.transactions.enums.PaymentTypeEnum;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.models.Transaction;
import com.example.gateway.commons.transactions.services.TransactionService;
import com.example.gateway.commons.utils.HttpUtil;
import com.example.gateway.commons.utils.PaymentUtils;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.budpay.dtos.MetaData;
import com.example.gateway.integrations.budpay.dtos.*;
import com.example.gateway.integrations.kora.dtos.card.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

import static com.example.gateway.commons.transactions.enums.Status.*;
import static javax.management.remote.JMXConnectionNotification.FAILED;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudPayService implements ICardService, ISettlementService {
    private final HttpUtil httpUtil;
    @Value("${budpay.baseurl}")
    private String budPayBaseUrl;
    @Value("${budpay.settlementUrl}")
    private String settlementUrl;
    @Value("${budpay.apiKey}")
    private String authorization;
    private final TransactionService transactionService;
    private final ChargeService chargeService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SettlementRepository settlementRepository;
    private final ObjectMapper objectMapper;
    @Override
    public Mono<Response<?>> payWithCard(CardPaymentRequestDTO cardPaymentRequestDTO, String merchantId, String userId) {
        String reference = cardPaymentRequestDTO.getTransactionReference().isEmpty()?UUID.randomUUID().toString().replace("-","").substring(0,30):cardPaymentRequestDTO.getTransactionReference();
        cardPaymentRequestDTO.setTransactionReference(reference);

        BudpayChargeCardRequest request = BudpayChargeCardRequest.builder()
                .data(ChargeCardData.builder()
                        .cvv(cardPaymentRequestDTO.getCard().getCvv())
                        .number(cardPaymentRequestDTO.getCard().getNumber())
                        .expiryMonth(cardPaymentRequestDTO.getCard().getExpiryMonth())
                        .expiryYear(cardPaymentRequestDTO.getCard().getExpiryYear())
                        .pin(cardPaymentRequestDTO.getCard().getPin())
                        .build())
                .reference(reference)
                .build();

        return httpUtil.post(budPayBaseUrl+"encrypt-data",request, Map.of("authorization","Bearer "+authorization,"content-type","application/json"),90)
                .flatMap(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()){
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(encryptedCardData -> {
                                    log.info("encrypted data response is {}",encryptedCardData);
                                    PaymentRequestDto paymentRequestDto = PaymentRequestDto.builder()
                                            .card(encryptedCardData.replace("\"",""))
                                            .currency(cardPaymentRequestDTO.getCurrency())
                                            .amount(cardPaymentRequestDTO.getAmount().toString())
                                            .reference(reference)
                                            .email(cardPaymentRequestDTO.getCustomerDetails().getEmail()==null?"":cardPaymentRequestDTO.getCustomerDetails().getEmail())
                                            .enforceSecureAuth(false)
                                            .build();
                                    try{
                                        String s = new ObjectMapper().writeValueAsString(paymentRequestDto);
                                        log.info("outgoing payload {}",s);

                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return saveTransaction(cardPaymentRequestDTO,merchantId,userId)
                                            .flatMap(transaction -> {
                                                return httpUtil.post(budPayBaseUrl+"direct-card-charge",paymentRequestDto,Map.of("authorization","Bearer "+authorization),90)
                                                        .flatMap(clientResponse1 -> {
                                                            if (clientResponse.statusCode().is2xxSuccessful()){
                                                                return clientResponse1.bodyToMono(Map.class)
                                                                        .flatMap(map -> {
                                                                            boolean status = (boolean) map.get("status");
                                                                            Transaction tranlog = (Transaction) transaction;
                                                                            if (!status){
                                                                                tranlog.setMetaData(map.get("message").toString());
                                                                                return updateTransaction(tranlog,Status.FAILED)
                                                                                        .flatMap(this::getResponseMono);
                                                                            }
                                                                            else {
                                                                                log.info("response is {}",map.toString());
                                                                                if (map.containsKey("data")){
                                                                                    Map data = objectMapper.convertValue(map.get("data"), Map.class);
                                                                                    data.put("reference",reference);
                                                                                    String authMode = data.get("authMode").toString();
                                                                                    switch (authMode){
                                                                                        case "3ds"->{
                                                                                            return updateTransaction(tranlog, PROCESSING)
                                                                                                    .flatMap(transaction1 -> process3DS(transaction1,data));
                                                                                        }
                                                                                        case "pin","Pin","PIN"->{
                                                                                            return updateTransaction(tranlog, PROCESSING)
                                                                                                    .flatMap(this::getResponseMono);
                                                                                        }
                                                                                        default -> {
                                                                                            return httpUtil.get(budPayBaseUrl + "transaction/" + reference + "/verify", Map.of(), 80)
                                                                                                    .flatMap(clientResponse2 -> {
                                                                                                        if (clientResponse2.statusCode().is2xxSuccessful()){
                                                                                                            return clientResponse2.bodyToMono(TransactionDetailsResponse.class)
                                                                                                                    .flatMap(transactionDetailsResponse -> {
                                                                                                                        if (transactionDetailsResponse.isStatus() && Objects.equals(transactionDetailsResponse.getData().getGatewayResponseCode(), "00"))
                                                                                                                            return updateTransaction(tranlog, SUCCESSFUL)
                                                                                                                                    .flatMap(this::getResponseMono);
                                                                                                                        return updateTransaction(tranlog,Status.PROCESSING)
                                                                                                                                .flatMap(this::getResponseMono);
                                                                                                                    });
                                                                                                        }
                                                                                                        else{
                                                                                                            return updateTransaction(tranlog,Status.PROCESSING)
                                                                                                                    .flatMap(this::getResponseMono);
                                                                                                        }
                                                                                                    });
                                                                                        }
                                                                                    }
                                                                                }
                                                                                //very weird case from budpay API
                                                                                else if (map.get("message").toString().equalsIgnoreCase("Payment Successful")){
                                                                                    return updateTransaction(tranlog, SUCCESSFUL)
                                                                                            .flatMap(this::getResponseMono);
                                                                                }
                                                                                else {
                                                                                    return updateTransaction(tranlog, PROCESSING)
                                                                                            .flatMap(this::getResponseMono);
                                                                                }



                                                                            }
                                                                        });
                                                            }
                                                            else {
                                                                return clientResponse1.bodyToMono(Map.class)
                                                                        .flatMap(map -> {
                                                                            log.error("error processing payment. error is {}",map);
                                                                            return Mono.error(new CustomException(Response.builder()
                                                                                    .message(FAILED)
                                                                                    .statusCode(HttpStatus.BAD_REQUEST.value())
                                                                                    .status(HttpStatus.BAD_REQUEST)
                                                                                    .errors(List.of(map.toString()))
                                                                                    .build(), HttpStatus.INTERNAL_SERVER_ERROR));
                                                                        });
                                                            }
                                                        });
                                            });

                                });
                    }
                    else {
                        return clientResponse.bodyToMono(Map.class)
                                .flatMap(map -> {
                                    log.error("error encrypting data for budpay. error is {}",map);
                                    return Mono.just(Response.builder()
                                            .message(FAILED)
                                            .statusCode(HttpStatus.BAD_REQUEST.value())
                                            .status(HttpStatus.BAD_REQUEST)
                                            .errors(List.of(map.toString()))
                                            .build());
                                });
                    }

                });

    }

    private Mono<Response<Object>> getResponseMono(Transaction transaction1) {
        KoraChargeCardResponse koraChargeCardResponse = new KoraChargeCardResponse();
        koraChargeCardResponse.setStatus(true);
        koraChargeCardResponse.setTransaction(transaction1);
        koraChargeCardResponse.setMessage("Card charged successfully");
        Data data = new Data();
        data.setAmount(transaction1.getAmount());
        data.setCurrency(transaction1.getCurrency());
        data.setFee(transaction1.getFee());
        data.setStatus("success");
        data.setPaymentReference(transaction1.getProviderReference());
        data.setTransactionReference(transaction1.getTransactionReference());
        data.setAmountCharged(Double.parseDouble(data.getAmount().toString()));
        data.setAuthModel("NO_AUTH");
        koraChargeCardResponse.setData(data);
        notificationService.postNotification(transaction1);
        return Mono.just(Response.builder()
                .data(koraChargeCardResponse)
                .message(SUCCESSFUL.name())
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
    private Mono<Response<Object>> process3DS(Transaction transaction1,Map extraParams) {
        KoraChargeCardResponse koraChargeCardResponse = new KoraChargeCardResponse();
        koraChargeCardResponse.setStatus(true);
        koraChargeCardResponse.setTransaction(transaction1);
        koraChargeCardResponse.setMessage("Charge in progress");
        Data data = new Data();
        data.setAmount(transaction1.getAmount());
        data.setCurrency(transaction1.getCurrency());
        data.setFee(transaction1.getFee());
        data.setStatus("processing");
        data.setPaymentReference(transaction1.getProviderReference());
        data.setTransactionReference(transaction1.getTransactionReference());
        data.setAmountCharged(Double.parseDouble(data.getAmount().toString()));
        data.setAuthModel("3DS_REDIRECT");
        data.setExtraParams(extraParams);
        koraChargeCardResponse.setData(data);
        notificationService.postNotification(transaction1);
        return Mono.just(Response.builder()
                .data(koraChargeCardResponse)
                .message(SUCCESSFUL.name())
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Override
    public Mono<Response<Object>> authorizeCard(AuthorizeCardRequestPin request, String merchantId, String userId) {
        return null;
    }

    @Override
    public Mono<Response<Object>> authorizeCardOtp(AuthorizeCardRequestOTP request, String merchantId, String userId) {
        return null;
    }

    @Override
    public Mono<Response<Object>> authorizeCardAvs(AuthorizeCardRequestAVS request, String merchantId, String userId) {
        return null;
    }

    @Override
    public Mono<Response<Object>> authorizeCardPhone(AuthorizeCardRequestPhone request, String merchantId, String userId) {
        return null;
    }

    @Override
    public String getName() {
        return "BUDPAY";
    }

    private Mono<Object> saveTransaction(CardPaymentRequestDTO request, String merchantId,String customerId){
        Transaction transaction = Transaction.builder()
                .pan(request.getCard().getNumber().substring(0,6).concat("******").concat(request.getCard().getNumber().substring(request.getCard().getNumber().length()-4)))
                .uuid(UUID.randomUUID().toString())
                .amount(request.getAmount())
                .userId(customerId)
                .paymentType(PaymentTypeEnum.CARD)
                .cardScheme(PaymentUtils.detectCardScheme(request.getCard().getNumber()))
                .transactionReference(request.getTransactionReference())
                .vestraPayReference(UUID.randomUUID().toString())
                .transactionStatus(Status.ONGOING)
                .currency(request.getCurrency())
                .narration("Transaction initiated at "+new Date()+ "by "+request.getCustomerDetails().getName())
                .activityStatus(Status.ONGOING.toString())
                .merchantId(merchantId)
                .providerName(getName())
                .build();
        return chargeService.getPaymentCharge(transaction.getMerchantId(), PaymentMethod.CARD, ChargeCategory.PAY_IN,transaction.getAmount(), request.getCurrency())
                .flatMap(fee -> {
                    transaction.setFee(fee);
                    return transactionService.saveTransaction(transaction,PaymentTypeEnum.CARD,merchantId);

                });

    }
    private Mono<Transaction> updateTransaction(Transaction request, Status status){
        request.setTransactionStatus(status);
        return transactionService.updateTransaction(request);
    }


    @Override
    public Mono<Transaction> pushSettlement(Transaction transaction, Settlement settlementAccount) {
        String bankName = settlementAccount.getBankName();
        List<Bank> data = ListBanksService.getBanks().getData();
        Bank settlementBank = data.stream().filter(bank -> bank.getName().equalsIgnoreCase(bankName))
                .findFirst().orElseThrow(() -> new RuntimeException("Settlement bank not found"));

        return userRepository.findByMerchantIdAndUserType(transaction.getMerchantId(), UserType.MERCHANT)
                .flatMap(user -> {
                    transaction.setSettlementReference(UUID.randomUUID().toString());
                    TransferRequest request = TransferRequest.builder()
                            .accountNumber(settlementAccount.getAccountNumber())
                            .bankName(settlementBank.getName())
                            .bankCode(settlementBank.getNibssBankCode())
                            .narration("transfer from vestrapay")
                            .amount(transaction.getAmount().subtract(transaction.getFee()))
                            .currency(transaction.getCurrency())
                            .metaData(List.of(MetaData.builder()
                                            .senderName("vestrapay")
                                            .senderAddress("vestrapay")
                                    .build()))
                            .build();
                    log.info("outgoing settlement request body is {}",request);
                    return httpUtil.post(settlementUrl,request,Map.of("Authorization",String.format("Bearer %s",authorization),
                                    "Encryption","Signature_HMAC-SHA-512",
                                    "Content-Type","application/json"),10000)
                            .flatMap(clientResponse -> {
                                if (clientResponse.statusCode().is2xxSuccessful()){
                                    return clientResponse.bodyToMono(TransferResponse.class)
                                            .flatMap(response -> {
                                                log.info("settlement response from korapay is {}",response);

                                                if (response.isSuccess()){
                                                    //budpay returns processing. we need to confirm that processing is successful
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
        return getName();
    }
}
