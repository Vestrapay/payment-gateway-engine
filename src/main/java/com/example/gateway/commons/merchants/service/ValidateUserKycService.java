package com.example.gateway.commons.merchants.service;

import com.example.gateway.commons.merchants.enums.UserType;
import com.example.gateway.commons.merchants.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidateUserKycService {
    private final UserRepository userRepository;

    public Mono<Boolean> isKycComplete(String merchantId){
        return userRepository.findByMerchantIdAndUserType(merchantId, UserType.MERCHANT)
                .flatMap(user -> {
                    if (user.isKycCompleted())
                        return Mono.just(true);
                    return Mono.just(false);
                }).switchIfEmpty(Mono.defer(() -> {
                    log.error("merchant not found");
                    return Mono.just(false);
                })).onErrorResume(throwable -> {
                    return Mono.just(false);
                });
    }
}
