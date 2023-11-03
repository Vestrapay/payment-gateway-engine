package com.example.gateway.integrations.wemabank.utils;

import com.example.gateway.commons.utils.HttpUtil;
import com.example.gateway.commons.utils.RedisUtility;
import com.example.gateway.commons.utils.Response;
import com.example.gateway.integrations.wemabank.dtos.WemaTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WemaTokenUtils {
    @Value("${spring.profiles.active}")
    private String profile;
    @Value("${vendor.username}")
    String username;
    @Value("${vendor.password}")
    String password;
    @Value("${wema.base.url}")
    String baseUrl;
    private final HttpUtil httpUtil;
    private final RedisUtility redisUtility;

    public Mono<Response<WemaTokenResponse>>getToken(){
        //todo save the authheader to redis and refresh token to redis for performance
        String url = baseUrl.concat("api/Authentication/authenticate");
        log.info(url);
        return httpUtil.post(url, Map.of("username",username,"password",password))
                .flatMap(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()){
                        return clientResponse.bodyToMono(WemaTokenResponse.class)
                                .flatMap(map -> {
                                    return Mono.just(Response.<WemaTokenResponse>builder()
                                                    .data(map)
                                                    .statusCode(clientResponse.statusCode().value())
                                                    .status(HttpStatus.valueOf(clientResponse.statusCode().value()))
                                                    .message("Success")
                                            .build());
                                });
                    }
                    else {
                        return Mono.just(Response.<WemaTokenResponse>builder()
                                .statusCode(clientResponse.statusCode().value())
                                .status(HttpStatus.valueOf(clientResponse.statusCode().value()))
                                .message("Failed")
                                .build());
                    }
                });

    }

//    @Scheduled(fixedRate = 36000)
    public void refreshToken(){
        log.info("refreshing token");
        if (profile.equalsIgnoreCase("jobs")){
            getToken().flatMap(wemaTokenResponseResponse -> {
                if (wemaTokenResponseResponse.getStatus().is2xxSuccessful()){
                    String token = wemaTokenResponseResponse.getData().getToken();
                    redisUtility.setValue("TOKEN",token,60);
                }
                return Mono.empty();
            }).subscribe();
        }

    }
}
