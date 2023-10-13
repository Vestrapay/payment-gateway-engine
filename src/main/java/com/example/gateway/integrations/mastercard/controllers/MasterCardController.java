package com.example.gateway.integrations.mastercard.controllers;

import com.example.gateway.commons.cardpayment.PaymentByCardRequestVO;
import com.example.gateway.commons.cardpayment.PaymentByCardResponseVO;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.mastercard.dto.MasterCardUpdateSessionResponse;
import com.example.gateway.integrations.mastercard.interfaces.IMasterCardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/mastercard")
@Tag(name = "MASTERCARD", description = "Mastercard Service Management")
@SecurityRequirement(name = "vestrapay")
@CrossOrigin(origins ="*",maxAge = 3600)
@RequiredArgsConstructor
public class MasterCardController {
    private final IMasterCardService masterCardService;
    @PostMapping("create-request")
    public Mono<ResponseEntity<Response<PaymentByCardResponseVO>>> makePayment(@RequestBody PaymentByCardRequestVO request){
        return masterCardService.makePayment(request)
                .map(response -> ResponseEntity.status(response.getStatus()).body(response));

    }





}
