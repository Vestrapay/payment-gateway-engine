package com.example.gateway.api.transfer.controllers;

import com.example.gateway.api.transfer.dtos.TransferPaymentRequestDTO;
import com.example.gateway.api.transfer.services.TransferPaymentService;
import com.example.gateway.commons.utils.Response;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/pay/transfer")
@Tag(name = "PAYMENT", description = "Payment Service Management")
@SecurityRequirement(name = "vestrapay")
@CrossOrigin(origins ="*",maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class TransferPaymentController {
    private final TransferPaymentService transferService;
    @PostMapping
    public Mono<ResponseEntity<Response<?>>> payWithTransfer(@RequestHeader String secret,
                                                             @RequestHeader String merchantId,
                                                             @RequestHeader @NotEmpty String userId,
                                                             @RequestBody @Valid TransferPaymentRequestDTO request){
        return transferService.payWithTransfer(secret,merchantId,request,userId)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @GetMapping("transaction-status/{transactionReference}")
    public Mono<ResponseEntity<Response<?>>>doTSQ(@RequestHeader String secret,
                                                  @RequestHeader String merchantId,
                                                  @RequestHeader @NotEmpty String userId,
                                                  @PathVariable("transactionReference") String reference){
        return transferService.doTSQ(secret,merchantId,reference,userId )
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

}
