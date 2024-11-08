package com.example.gateway.api.transfer.router;

import com.example.gateway.api.transfer.dtos.TransferPaymentRequestDTO;
import com.example.gateway.commons.repository.RoutingRuleRepository;
import com.example.gateway.commons.utils.ObjectMapperUtil;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.transfer.PayWithTransferDTO;
import com.example.gateway.integrations.kora.interfaces.IKoraService;
import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
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
public class TransferPaymentRouter {
    private final IKoraService koraService;
    private final IWemaBankService wemaBankService;
    private final RoutingRuleRepository routingRuleRepository;
    @Value("${default.provider}")
    String defaultProvider;

    public Mono<Response<Object>> routePayment(TransferPaymentRequestDTO transferPaymentRequestDTO, String merchantId, String customerId){
        PayWithTransferDTO transferRequestDTO = ObjectMapperUtil.toKoraPayTransferDTO(transferPaymentRequestDTO);
        log.info("incoming transfer payment request via korapay is {}",transferRequestDTO);
        return routingRuleRepository.findByPaymentMethod(PaymentTypeEnum.TRANSFER.name())
                .flatMap(routingRule -> {
                    if (routingRule.getProvider().equalsIgnoreCase("KORAPAY")){
                        // TODO: 15/09/2024 calculate the fee before saving
                        return koraService.payWithTransfer(transferRequestDTO,merchantId,customerId).flatMap(objectResponse -> {
                            return Mono.just(Response.builder()
                                    .data(objectResponse.getData())
                                    .statusCode(objectResponse.getStatusCode())
                                    .status(objectResponse.getStatus())
                                    .message(objectResponse.getMessage())
                                    .errors(objectResponse.getErrors())
                                    .build());
                        });
                    } else if (routingRule.getProvider().equalsIgnoreCase("WEMA")) {
                        return wemaBankService.payWithTransfer(merchantId,transferRequestDTO,customerId)
                                .flatMap(wemaAccountsResponse -> {
                                    return Mono.just(Response.builder()
                                            .data(wemaAccountsResponse.getData())
                                            .errors(wemaAccountsResponse.getErrors())
                                            .message(wemaAccountsResponse.getMessage())
                                            .status(wemaAccountsResponse.getStatus())
                                            .statusCode(wemaAccountsResponse.getStatusCode())
                                            .build());
                                });
                    }
                    else {
                        log.error("no implementation for provider {}",routingRule.getProvider());
                        return Mono.just(Response.builder()
                                .errors(List.of("no routing rule for transfer"))
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .message("Failed")
                                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .build());
                    }


                })
                .switchIfEmpty(Mono.defer(() -> {
                    return Mono.just(Response.builder()
                            .errors(List.of("no implementation for provider"))
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Failed")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
                }));
    }

}
