package com.example.gateway.integrations.sandbox;

import com.example.gateway.commons.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class SandboxEnvironment {
    public Mono<Response<?>> getResponse(Object response){
        return Mono.just(Response.builder()
                        .data(response)
                        .message("SUCCESSFUL")
                        .statusCode(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                .build());
    }
}
