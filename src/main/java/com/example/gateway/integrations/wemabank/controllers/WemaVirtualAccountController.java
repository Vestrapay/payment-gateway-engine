package com.example.gateway.integrations.wemabank.controllers;

import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.wemabank.dtos.*;
import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/wemabank/virtual-account")
@Tag(name = "WEMABANK VIRTUAL ACCOUNT", description = "Mastercard Service Management")
@SecurityRequirement(name = "vestrapay")
@CrossOrigin(origins ="*",maxAge = 3600)
@RequiredArgsConstructor
public class WemaVirtualAccountController {
    private final IWemaBankService wemaBankService;
    @PostMapping("wema-notify")
    public Mono<ResponseEntity<WemaTransactionNotificationResponse>> wemaNotificationWebhook(@RequestBody WemaTransactionDTO request){
        return wemaBankService.receiveTransactionNotification(request)
                .map(wemaTransactionNotificationResponse -> ResponseEntity.status(HttpStatus.OK).body(wemaTransactionNotificationResponse));

    }

    @PostMapping("account-lookup")
    public Mono<ResponseEntity<WemaAccountLookupResponse>> accountLookup(@RequestBody AccountLookupRequest request){
        return wemaBankService.accountLookup(request)
                .map(wemaTransactionNotificationResponse -> ResponseEntity.status(HttpStatus.OK).body(wemaTransactionNotificationResponse));

    }

    @PostMapping("transaction-query")
    public Mono<ResponseEntity<WemaTransactionDTO>> transactionQuery(@RequestBody WemaTransactionQueryRequest request){
        return wemaBankService.tranasctionQuery(request)
                .map(wemaTransactionNotificationResponse -> ResponseEntity.status(HttpStatus.OK).body(wemaTransactionNotificationResponse));

    }
}
