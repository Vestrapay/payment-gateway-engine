package com.example.gateway.commons.webhook.controllers;

import com.example.gateway.commons.webhook.service.WebhookService;
import com.example.gateway.commons.utils.Response;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/pay/webhook")
@Tag(name = "PAYMENT", description = "Payment Service Management")
@SecurityRequirement(name = "vestrapay")
@CrossOrigin(origins ="*",maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class WebHookController {
    private final WebhookService webhookService;

    // TODO: 05/08/2024 make endpoint only accessible by provider ip CIDR range
    @PostMapping("{PROVIDER}")
    public Mono<ResponseEntity<Response<Object>>> webhook(@PathVariable("PROVIDER")String provider, @RequestBody @Valid Object request){
        log.info("incoming webhook request is {} and provider is {}",request,provider);
        return webhookService.process(request,provider)
                .map(cardPaymentResponseDTOResponse -> ResponseEntity.status(cardPaymentResponseDTOResponse.getStatus())
                        .body(cardPaymentResponseDTOResponse));
    }
}
