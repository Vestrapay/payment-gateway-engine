package com.example.gateway.integrations.mastercard.service;

import com.example.gateway.commons.charge.enums.ChargeCategory;
import com.example.gateway.commons.charge.enums.PaymentMethod;
import com.example.gateway.commons.charge.service.ChargeService;
import com.example.gateway.commons.notificatioin.NotificationService;
import com.example.gateway.integrations.kora.dtos.card.Data;
import com.example.gateway.integrations.kora.dtos.card.KoraChargeCardResponse;
import com.example.gateway.integrations.mastercard.dto.PaymentByCardRequestVO;
import com.example.gateway.integrations.mastercard.dto.PaymentByCardResponseVO;
import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.utils.PaymentUtils;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.PaymentTypeInterface;
import com.example.gateway.integrations.mastercard.dto.*;
import com.example.gateway.integrations.mastercard.interfaces.IMasterCardService;
import com.example.gateway.commons.transactions.enums.PaymentTypeEnum;
import com.example.gateway.commons.transactions.enums.Status;
import com.example.gateway.commons.transactions.models.Transaction;
import com.example.gateway.commons.transactions.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Qualifier("masterCardService")
@Primary
public class MasterCardService implements IMasterCardService, PaymentTypeInterface {
    private static final String FAILED = "FAILED";
    private static final String SUCCESSFUL = "SUCCESSFUL";
    private final MpgsService mpgsService;
    private final TransactionService transactionService;

    @Value("${mpgs.merchantId}")
    private String vestraPayMerchantID;
    private final ChargeService chargeService;
    private final NotificationService notificationService;
    @Override
    public Mono<Response<?>> makePayment(PaymentByCardRequestVO requestVO, String customerId) {
        if (requestVO.getMerchantId().isEmpty()){
            log.info("merchant Id must not be blank");
            throw new CustomException(Response.builder()
                    .message(FAILED)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .errors(List.of("merchant id not present in request"))
                    .build(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return saveTransaction(requestVO,customerId)
                .flatMap(transaction -> mpgsService.createSession(vestraPayMerchantID)
                        .flatMap(masterCardCreateSessionResponse -> {
                            mpgsService.updateSession(createSessionRequest(requestVO),masterCardCreateSessionResponse.getSession().getId(),vestraPayMerchantID).subscribe();

                            return mpgsService.makePayment(createMakePaymentRequest(requestVO,masterCardCreateSessionResponse.getSession().getId()),vestraPayMerchantID)
                                    .flatMap(response -> {
                                        log.info("response from MPGS is {}",response.toString());
                                        Status status;
                                        if (response.getResult().equalsIgnoreCase("Failure")){
                                             status= Status.FAILED;
                                        }
                                        else {
                                            if (response.getResponse().getAcquirerCode().equalsIgnoreCase("00")){
                                                status = Status.SUCCESSFUL;
                                                Transaction tranlog = (Transaction) transaction;
                                                return updateTransaction(tranlog,status)
                                                        .flatMap(transaction1 -> {
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
                                                            data.setAuthModel(response.getResponse().getGatewayRecommendation());
                                                            koraChargeCardResponse.setData(data);
                                                            notificationService.postNotification(transaction1);
                                                            return Mono.just(Response.builder()
                                                                    .data(koraChargeCardResponse)
                                                                    .message(SUCCESSFUL)
                                                                    .statusCode(HttpStatus.OK.value())
                                                                    .status(HttpStatus.OK)
                                                                    .build());
                                                        });
                                            }
                                            else
                                                status = Status.FAILED;
                                        }
                                        Transaction tranLog = (Transaction) transaction;
                                        return updateTransaction(tranLog,status)
                                                .flatMap(transaction1 -> {
                                                    notificationService.postNotification(transaction1);
                                                    return Mono.just(Response.<MasterCardMakePaymentResponse>builder()
                                                            .data(response)
                                                            .message(FAILED)
                                                            .statusCode(HttpStatus.EXPECTATION_FAILED.value())
                                                            .status(HttpStatus.EXPECTATION_FAILED)
                                                            .build());
                                                });



                                    })
                                    .switchIfEmpty(Mono.defer(() -> {
                                        log.error("error making payment for request {}", requestVO);
                                        Transaction tranLog = (Transaction) transaction;
                                        return updateTransaction(tranLog,Status.ONGOING)
                                                .flatMap(transaction1 -> {
                                                    log.error("no response from gateway");
                                                    notificationService.postNotification(transaction1);
                                                    return Mono.just(Response.<MasterCardMakePaymentResponse>builder()
                                                                    .data(null)
                                                                    .message(FAILED)
                                                                    .status(HttpStatus.REQUEST_TIMEOUT)
                                                                    .statusCode(HttpStatus.REQUEST_TIMEOUT.value())
                                                                    .errors(List.of("Response not received","perform TSQ"))
                                                            .build());
                                                });
                                    }));



                        }).switchIfEmpty(Mono.defer(() -> {
                            log.error("unable to create session for transaction {}, cancelling transaction", requestVO);
                            Transaction tranLog = (Transaction) transaction;
                            updateTransaction(tranLog,Status.FAILED).subscribe();
                            notificationService.postNotification(tranLog);
                            throw new CustomException(Response.builder()
                                    .message(FAILED)
                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .errors(List.of("unable to create session for transaction"))
                                    .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                        })).doOnError(CustomException.class, customException -> {
                            Transaction tranlog = (Transaction) transaction;
                            updateTransaction(tranlog,Status.FAILED).subscribe();
                            notificationService.postNotification(tranlog);
                            String errorList = "";
                            if (Objects.nonNull(customException.getResponse()) && Objects.nonNull(customException.getResponse().getData())){
                                errorList = customException.getResponse().getData().toString();

                            }
                            throw new CustomException(Response.builder()
                                    .message(FAILED)
                                    .statusCode(HttpStatus.BAD_REQUEST.value())
                                    .status(HttpStatus.BAD_REQUEST)
                                    .data(customException.getMessage())
                                    .errors(List.of(customException.getMessage() == null ? errorList: customException.getMessage()))
                                    .build(), customException.getHttpStatus());

                        }))
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("error saving initial transaction");
                    return Mono.error(new CustomException(Response.builder()
                            .statusCode(HttpStatus.CONFLICT.value())
                            .message("Attempt for duplicate request forbidden")
                            .build(), HttpStatus.CONFLICT));
                }))
                .doOnError(CustomException.class, customException -> {
                    throw new CustomException(customException.getResponse(), customException.getResponse().getStatus());
                });

    }

    @Override
    public Mono<Response<PaymentByCardResponseVO>> doTsq() {
        return null;
    }

    private Mono<Object> saveTransaction(PaymentByCardRequestVO request, String customerId){
        Transaction transaction = Transaction.builder()
                .pan(request.getCardDetails().getPan().substring(0,6).concat("******").concat(request.getCardDetails().getPan().substring(request.getCardDetails().getPan().length()-4)))
                .uuid(UUID.randomUUID().toString())
                .amount(new BigDecimal(request.getAmount()))
                .userId(customerId)
                .paymentType(PaymentTypeEnum.CARD)
                .cardScheme(PaymentUtils.detectCardScheme(request.getCardDetails().getPan()))
                .transactionReference(request.getReference())
                .vestraPayReference(UUID.randomUUID().toString())
                .transactionStatus(Status.ONGOING)
                .currency(request.getCurrency().getCode())
                .narration("Transaction initiated at "+new Date())
                .activityStatus(Status.ONGOING.toString())
                .merchantId(request.getMerchantId())
                .providerName("MPGS")
                .build();
        return chargeService.getPaymentCharge(transaction.getMerchantId(), PaymentMethod.CARD, ChargeCategory.PAY_IN,transaction.getAmount())
                .flatMap(fee -> {
                    transaction.setFee(fee);
                    return transactionService.saveTransaction(transaction,PaymentTypeEnum.CARD,request.getMerchantId());

                });

    }

    private Mono<Transaction> updateTransaction(Transaction request, Status status){
        request.setTransactionStatus(status);
        return transactionService.updateTransaction(request);
    }

    private static MasterCardUpdateSessionRequest createSessionRequest(PaymentByCardRequestVO request){
        return MasterCardUpdateSessionRequest.builder()
                .order(MasterCardOrder.builder()
                        .amount(new BigDecimal(request.getAmount()))
                        .currency(request.getCurrency().getCode())
                        .reference(request.getReference())
                        .build())
                .build();
    }

    private static MasterCardMakePaymentRequest createMakePaymentRequest(PaymentByCardRequestVO request,String sessiongId){
        final String[] expiryMonthAndYear = retrieveExpiryMonthAndYear(request.getCardDetails().getExpiryDate());
        MasterCardSourceOfFunds sourceOfFunds = MasterCardSourceOfFunds.builder()
                .provided(ProvidedDto.builder()
                        .card(CardDto.builder()
                                .expiry(ExpiryDto.builder()
                                        .month(expiryMonthAndYear[0])
                                        .year(expiryMonthAndYear[1])
                                        .build())
                                .number(request.getCardDetails().getPan())
                                .securityCode(request.getCardDetails().getCvv2())
                                .build())
                        .build())
                .type("CARD")
                .build();

        return MasterCardMakePaymentRequest.builder()
                .apiOperation("PAY")
                .order(createMasterCardOrder(request))
                .sourceOfFunds(sourceOfFunds)
                .session(MasterCardSession.builder().id(sessiongId).build())
                .transaction(MasterCardTransaction.builder().reference(request.getReference()).build())
                .build();
    }

    private static String[] retrieveExpiryMonthAndYear(String expiryDate){
        if (expiryDate.length()!=4){
            throw new CustomException("Invalid expiryDate");
        }
        return new String[]{expiryDate.substring(0,2),expiryDate.substring(2,4)};
    }

    private static MasterCardOrder createMasterCardOrder(PaymentByCardRequestVO request){
        return MasterCardOrder.builder()
                .amount(new BigDecimal(request.getAmount()))
                .currency(request.getCurrency().getCode())
                .reference(request.getReference())
                .build();
    }

    @Override
    public String getPaymentType() {
        return "CARD";
    }
}
