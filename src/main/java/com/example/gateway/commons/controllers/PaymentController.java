package com.example.gateway.commons.controllers;

import com.example.gateway.commons.dto.card.CardPaymentRequestDTO;
import com.example.gateway.commons.dto.card.CardPaymentResponseDTO;
import com.example.gateway.commons.dto.paymentlink.PaymentLinkRequestDTO;
import com.example.gateway.commons.dto.transfer.TransferPaymentRequestDTO;
import com.example.gateway.commons.dto.ussd.USSDPaymentRequestDTO;
import com.example.gateway.commons.interfaces.IPaymentService;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestAVS;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestOTP;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestPhone;
import com.example.gateway.integrations.kora.dtos.card.AuthorizeCardRequestPin;
import com.example.gateway.integrations.kora.interfaces.IKoraService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("api/v1/pay/")
@Tag(name = "PAYMENT", description = "Payment Service Management")
@SecurityRequirement(name = "vestrapay")
@CrossOrigin(origins ="*",maxAge = 3600)
@RequiredArgsConstructor
public class PaymentController {
    private final IPaymentService paymentService;
    private final IKoraService koraService;
    @PostMapping("card")
    public Mono<ResponseEntity<Response<?>>>payWithCard(@RequestHeader String secret,
                                                                             @RequestHeader String merchantId,
                                                                             @RequestBody @Valid CardPaymentRequestDTO request){
        return paymentService.payWithCard(secret,merchantId,request)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("authorize-pin")
    public Mono<ResponseEntity<Response<?>>>authorizePin(@RequestHeader String secret,
                                                        @RequestHeader String merchantId,
                                                        @RequestBody @Valid AuthorizeCardRequestPin request){
        return koraService.authorizeCard(request,merchantId)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("authorize-otp")
    public Mono<ResponseEntity<Response<?>>>authorizeOtp(@RequestHeader String secret,
                                                         @RequestHeader String merchantId,
                                                         @RequestBody @Valid AuthorizeCardRequestOTP request){
        return koraService.authorizeCardOtp(request,merchantId)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("authorize-phone")
    public Mono<ResponseEntity<Response<?>>>authorizeOtp(@RequestHeader String secret,
                                                         @RequestHeader String merchantId,
                                                         @RequestBody @Valid AuthorizeCardRequestPhone request){
        return koraService.authorizeCardPhone(request,merchantId)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("authorize-avs")
    public Mono<ResponseEntity<Response<?>>>authorizeAvs(@RequestHeader String secret,
                                                         @RequestHeader String merchantId,
                                                         @RequestBody @Valid AuthorizeCardRequestAVS request){
        return koraService.authorizeCardAvs(request,merchantId)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }



    @PostMapping("transfer")
    public Mono<ResponseEntity<Response<?>>>payWithTransfer(@RequestHeader String secret,
                                                                             @RequestHeader String merchantId,
                                                                             @RequestBody @Valid TransferPaymentRequestDTO request){
        return paymentService.payWithTransfer(secret,merchantId,request)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("ussd")
    public Mono<ResponseEntity<Response<?>>>payWithUSSD(@RequestHeader String secret,
                                                                                 @RequestHeader String merchantId,
                                                                                 @RequestBody @Valid USSDPaymentRequestDTO request){
        return paymentService.payWithUSSD(secret,merchantId,request)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }

    @PostMapping("paymentlink")
    public Mono<ResponseEntity<Response<?>>>payWithUSSD(@RequestHeader String secret,
                                                                             @RequestHeader String merchantId,
                                                                             @RequestBody @Valid PaymentLinkRequestDTO request){
        return paymentService.payWithPaymentLink(secret,merchantId,request)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }


    @PostMapping("webhook")
    public Mono<ResponseEntity<Response<Object>>>webhook(@RequestBody @Valid Map<String,Object> request){
        return paymentService.webhook(request)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }
}
