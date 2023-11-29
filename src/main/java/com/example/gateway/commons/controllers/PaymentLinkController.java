package com.example.gateway.commons.controllers;

import com.example.gateway.commons.keys.models.Keys;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.paymentlink.dto.PaymentLinkDTO;
import com.example.gateway.integrations.paymentlink.interfaces.IPaymentLinkService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Tag(name = "PAYMENT LINK",description = "payment link description")
@SecurityRequirement(name = "vestrapay")
@CrossOrigin(origins ="*",maxAge = 3600)
@RequestMapping("api/v1/payment-link")
public class PaymentLinkController {
    private final IPaymentLinkService paymentLinkService;

    @PostMapping("generate")
    public Mono<ResponseEntity<Response<?>>>generateLink(@RequestBody PaymentLinkDTO request,
                                                         @RequestHeader String secret,
                                                         @RequestHeader String merchantId){
        return paymentLinkService.generatePaymentLink(merchantId,secret,request)
                .map(response -> ResponseEntity.status(response.getStatus()).body(response));

    }

    @GetMapping("view-all")
    public Mono<ResponseEntity<Response<?>>>viewAll(@RequestHeader String secret,
                                                         @RequestHeader String merchantId){
        return paymentLinkService.fetchAllLinksForMerchant(merchantId,secret)
                .map(response -> ResponseEntity.status(response.getStatus()).body(response));

    }

    @GetMapping("view-status")
    public Mono<ResponseEntity<Response<?>>>viewPaymentLinkStatus(@RequestHeader String linkId,
                                                   @RequestHeader String secret,
                                                   @RequestHeader String merchantId){
        return paymentLinkService.viewLinkStatus(merchantId,secret,linkId)
                .map(response -> ResponseEntity.status(response.getStatus()).body(response));

    }

    @GetMapping("paylink/{linkId}")
    public Mono<ResponseEntity<Response<?>>>viewPaymentLinkStatus(@PathVariable("linkId")String linkId){
        return paymentLinkService.getPaymentLinkDetails(linkId)
                .map(response -> {
                    Response<?> finalResponse = Response.builder()
                            .statusCode(response.getStatusCode())
                            .status(response.getStatus())
                            .errors(response.getErrors())
                            .message(response.getMessage())
                            .data(response.getData())
                            .build();

                    Keys keys=response.getKeys();
                    if (keys==null){
                        return ResponseEntity.status(finalResponse.getStatus())
                                .header("Access-Control-Expose-Headers","*")
                                .header("merchant_Id","")
                                .header("merchant_secret","")
                                .body(finalResponse);
                    }
                    else {
                        return ResponseEntity.status(finalResponse.getStatus())
                                .header("Access-Control-Expose-Headers","*")
                                .header("merchant_Id",keys.getUserId())
                                .header("merchant_secret",keys.getSecretKey())
                                .body(finalResponse);
                    }

                });

    }


}
