package com.example.gateway.api.card.router;

import com.example.gateway.api.card.dtos.CardPaymentRequestDTO;
import com.example.gateway.commons.repository.RoutingRuleRepository;
import com.example.gateway.commons.utils.ObjectMapperUtil;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.Customer;
import com.example.gateway.integrations.kora.dtos.card.KoraPayWithCardRequest;
import com.example.gateway.integrations.kora.interfaces.IKoraService;
import com.example.gateway.integrations.mastercard.dto.PaymentByCardRequestVO;
import com.example.gateway.integrations.mastercard.interfaces.IMasterCardService;
import com.example.gateway.commons.transactions.enums.PaymentTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardPaymentRouter {
    private final IMasterCardService masterCardService;
    private final IKoraService koraService;
    private final RoutingRuleRepository routingRuleRepository;
    @Value("${default.provider}")
    String defaultProvider;

    public Mono<Response<?>>routePayment(CardPaymentRequestDTO cardPaymentRequestDTO, String merchantId, String customerId){
        log.info("incoming payment request {}",cardPaymentRequestDTO);
        return routingRuleRepository.findByPaymentMethod(PaymentTypeEnum.CARD.name())
                .flatMap(routingRule -> {
                    if (routingRule.getProvider().equalsIgnoreCase("MASTERCARD")){
                        PaymentByCardRequestVO masterCardDTO = ObjectMapperUtil.toMasterCardDTO(cardPaymentRequestDTO, merchantId);
                        //convert to factory
                        return masterCardService.makePayment(masterCardDTO, customerId)
                                .flatMap(masterCardMakePaymentResponseResponse -> Mono.just(Response.builder()
                                        .data(masterCardMakePaymentResponseResponse.getData())
                                        .status(masterCardMakePaymentResponseResponse.getStatus())
                                        .errors(masterCardMakePaymentResponseResponse.getErrors())
                                        .message(masterCardMakePaymentResponseResponse.getMessage())
                                        .statusCode(masterCardMakePaymentResponseResponse.getStatusCode())
                                        .build()));
                    } else if (routingRule.getProvider().equalsIgnoreCase("KORAPAY")) {
                        KoraPayWithCardRequest koraPayCardDTO = ObjectMapperUtil.toKoraPayCardDTO(cardPaymentRequestDTO, merchantId);
                        Customer customer = new Customer(cardPaymentRequestDTO.getCard().getName(),cardPaymentRequestDTO.getCustomerDetails().getEmail()==null?"no-reply@vestrapay.com":cardPaymentRequestDTO.getCustomerDetails().getEmail()); //refactor to get email from environment
                        koraPayCardDTO.setCustomer(customer);
                        return koraService.payWithCard(koraPayCardDTO,merchantId,customerId);
                    }
                    log.error("no implementation for provider {}",routingRule.getProvider());
                    return Mono.just(Response.builder()
                            .errors(List.of("no implementation for provider {}"+routingRule.getProvider()))
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Failed")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
                }).switchIfEmpty(Mono.defer(() -> {
                    log.error("Routing rule not configured for gateway check configurations");
                    return Mono.just(Response.builder()
                            .errors(List.of("routing rule not configured for gateway."))
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Failed")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
                }));
    }

}
