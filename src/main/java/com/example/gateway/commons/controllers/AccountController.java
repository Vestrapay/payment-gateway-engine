package com.example.gateway.commons.controllers;

import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.wemabank.dtos.AccountLookupRequest;
import com.example.gateway.integrations.wemabank.interfaces.IWemaBankService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/accounts/")
@Tag(name = "VIRTUAL ACCOUNT SERVICE", description = "Virtual Account Service Management")
@SecurityRequirement(name = "vestrapay")
@CrossOrigin(origins ="*",maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    private final IWemaBankService wemaBankService;
    @PostMapping("name-enquiry")
    public Mono<ResponseEntity<?>> doNameEnquiry(@RequestBody AccountLookupRequest request){
        return wemaBankService.accountLookup(request)
                .map(ResponseEntity::ok);
    }
}
