package com.example.gateway.api.card.controllers;

import com.example.gateway.api.card.dtos.CardPaymentRequestDTO;
import com.example.gateway.api.card.services.CardPaymentService;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestAVS;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestOTP;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestPhone;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestPin;
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
@RequestMapping("api/v1/pay/card")
@Tag(name = "PAYMENT", description = "Payment Service Management")
@SecurityRequirement(name = "vestrapay")
@CrossOrigin(origins ="*",maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class CardPaymentController {
    private final CardPaymentService cardService;

    @PostMapping
    public Mono<ResponseEntity<Response<?>>> payWithCard(@RequestHeader String secret,
                                                         @RequestHeader String merchantId,
                                                         @RequestHeader  @NotEmpty String userId,
                                                         @RequestBody @Valid CardPaymentRequestDTO request){
        return cardService.payWithCard(secret,merchantId,request,userId )
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("authorize-pin")
    public Mono<ResponseEntity<Response<?>>>authorizePin(@RequestHeader String secret,
                                                         @RequestHeader String merchantId,
                                                         @RequestHeader @NotEmpty String userId,
                                                         @RequestBody @Valid AuthorizeCardRequestPin request){
        return cardService.authorizeCard(request,merchantId,userId)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("authorize-otp")
    public Mono<ResponseEntity<Response<?>>>authorizeOtp(@RequestHeader String secret,
                                                         @RequestHeader String merchantId,
                                                         @RequestHeader @NotEmpty String userId,
                                                         @RequestBody @Valid AuthorizeCardRequestOTP request){
        return cardService.authorizeCardOtp(request,merchantId,userId)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("authorize-phone")
    public Mono<ResponseEntity<Response<?>>>authorizeOtp(@RequestHeader String secret,
                                                         @RequestHeader String merchantId,
                                                         @RequestHeader @NotEmpty String userId,
                                                         @RequestBody @Valid AuthorizeCardRequestPhone request){
        return cardService.authorizeCardPhone(request,merchantId,userId)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("authorize-avs")
    public Mono<ResponseEntity<Response<?>>>authorizeAvs(@RequestHeader String secret,
                                                         @RequestHeader String merchantId,
                                                         @RequestHeader @NotEmpty String userId,
                                                         @RequestBody @Valid AuthorizeCardRequestAVS request){
        return cardService.authorizeCardAvs(request,merchantId,userId)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @GetMapping("transaction-status/{transactionReference}")
    public Mono<ResponseEntity<Response<?>>>doTSQ(@RequestHeader String secret,
                                                  @RequestHeader String merchantId,
                                                  @RequestHeader @NotEmpty String userId,
                                                  @PathVariable("transactionReference") String reference){
        return cardService.doTSQ(secret,merchantId,reference,userId )
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }


}
