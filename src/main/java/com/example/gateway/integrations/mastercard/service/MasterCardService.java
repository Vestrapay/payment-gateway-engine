package com.example.gateway.integrations.mastercard.service;

import com.example.gateway.commons.cardpayment.PaymentByCardRequestVO;
import com.example.gateway.commons.cardpayment.PaymentByCardResponseVO;
import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.mastercard.dto.*;
import com.example.gateway.integrations.mastercard.interfaces.IMasterCardService;
import com.example.gateway.transactions.enums.PaymentTypeEnum;
import com.example.gateway.transactions.enums.Status;
import com.example.gateway.transactions.models.Transaction;
import com.example.gateway.transactions.reporitory.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MasterCardService implements IMasterCardService {
    private static final String FAILED = "FAILED";
    private static final String SUCCESSFUL = "SUCCESSFUL";
    private final MpgsService mpgsService;
    private final TransactionRepository transactionRepository;
    @Override
    public Mono<Response<PaymentByCardResponseVO>> makePayment(PaymentByCardRequestVO requestVO) {
        if (requestVO.getMerchantId().isEmpty()){
            log.info("merchant Id must not be blank");
            throw new CustomException(Response.builder()
                    .message(FAILED)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .errors(List.of("merchant id not present in request"))
                    .build(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        //todo save initial transaction
        return saveTransaction(requestVO)
                .flatMap(transaction -> {
                    return mpgsService.createSession(transaction.getMerchantId())
                            .flatMap(masterCardCreateSessionResponse -> {
                                mpgsService.updateSession(createSessionRequest(requestVO),masterCardCreateSessionResponse.getSession().getId(),transaction.getMerchantId()).subscribe();

                                return mpgsService.makePayment(createMakePaymentRequest(requestVO,masterCardCreateSessionResponse.getSession().getId()),transaction.getMerchantId())
                                        .flatMap(response -> {
                                            log.info("response from MPGS is {}",response.toString());
                                            Status status = Status.valueOf(response.getResult());

                                            return updateTransaction(transaction,status)
                                                    .flatMap(transaction1 -> Mono.just(Response.<PaymentByCardResponseVO>builder()
                                                                    .message(SUCCESSFUL)
                                                                    .statusCode(HttpStatus.OK.value())
                                                                    .status(HttpStatus.OK)
                                                            .build()));
                                        })
                                        .switchIfEmpty(Mono.defer(() -> {
                                            log.error("error making payment for request {}", requestVO);
                                            return updateTransaction(transaction,Status.ONGOING)
                                                    .flatMap(transaction1 -> {
                                                        log.error("no response from gateway");
                                                        return Mono.just(Response.<PaymentByCardResponseVO>builder()
                                                                        .data(null) //todo create mapper from transaction to response DTO
                                                                        .message(SUCCESSFUL)
                                                                        .status(HttpStatus.REQUEST_TIMEOUT)
                                                                        .statusCode(HttpStatus.REQUEST_TIMEOUT.value())
                                                                        .errors(List.of("Response not received"))
                                                                .build());
                                                    });
                                        }));



                            }).switchIfEmpty(Mono.defer(() -> {
                                log.error("unable to create session for transaction {}, cancelling transaction", requestVO);
                                throw new CustomException(Response.builder()
                                        .message(FAILED)
                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .errors(List.of("unable to create session for transaction"))
                                        .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                            }))
                            .doOnError(throwable -> {
                                throw new CustomException(Response.builder()
                                        .message(FAILED)
                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .errors(List.of("unable to create session for transaction"))
                                        .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                            });


                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("error saving initial transaction");
                    throw new CustomException(Response.builder()
                            .message(FAILED)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .errors(List.of("unable to save initial transaction record"))
                            .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                }))
                .doOnError(throwable -> {
                    log.error("error saving initial transaction");
                    throw new CustomException(Response.builder()
                            .message(FAILED)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .errors(List.of(throwable.getLocalizedMessage()))
                            .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                });

    }

    @Override
    public Mono<Response<PaymentByCardResponseVO>> doTsq() {
        return null;
    }

    //todo add transaction fee to the transaction record
    private Mono<Transaction> saveTransaction(PaymentByCardRequestVO request){
        //todo save transaction
        Transaction transaction = Transaction.builder()
                .pan(request.getCardDetails().getPan().substring(0,6).concat("******").concat(request.getCardDetails().getPan().substring(request.getCardDetails().getPan().length()-4,request.getCardDetails().getPan().length())))
                .uuid(UUID.randomUUID().toString())
                .amount(new BigDecimal(request.getAmount()))
                .userId(request.getMerchantId())
                .paymentType(PaymentTypeEnum.CARD)
                .cardScheme(request.getCardDetails().getPan().substring(0,6))
                .transactionReference(UUID.randomUUID().toString())
                .transactionStatus(Status.ONGOING)
                .narration("Transaction initiated at "+new Date())
                .activityStatus(Status.ONGOING.toString())
//                .merchantId(request.getMerchantId())
                .build();
        return transactionRepository.save(transaction);
    }

    private Mono<Transaction> updateTransaction(Transaction request, Status status){
        //todo save transaction
        return transactionRepository.findByTransactionReferenceAndMerchantIdAndUuid(request.getTransactionReference(), request.getMerchantId(),request.getUuid())
                .flatMap(transaction -> {
                    transaction.setTransactionStatus(status);
                    return transactionRepository.save(transaction)
                            .flatMap(Mono::just);

                }).switchIfEmpty(Mono.defer(() -> {
                    return Mono.just(request);
                }));
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

    public String getProcessorName(){
        return "MASTERCARD";
    }
}
