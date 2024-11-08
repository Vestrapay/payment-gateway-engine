package com.example.gateway.commons.merchants.repository;


import com.example.gateway.commons.merchants.enums.UserType;
import com.example.gateway.commons.merchants.models.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
public interface UserRepository extends R2dbcRepository<User,Long> {

    Mono<User>findByMerchantIdAndUserType(String merchantId, UserType userType);


}
