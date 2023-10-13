package com.example.gateway.commons.utils;

import com.example.gateway.commons.exceptions.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpUtil {
    private final WebClient.Builder webClientBuilder;
    private static final Long DEFAULT_TIME_OUT = 30000L;
    private final MessageProvider messageProvider;


    public Mono<ClientResponse> get(String endpoint, Map<String, String> headers, int timeOut) {
        return webClientBuilder
                .build()
                .get()
                .uri(endpoint)
                .headers(generateHttpHeaders(headers))
                .exchange()
                .onErrorResume(this::handleErrorResume)
                .timeout(Duration.ofMillis(timeOut));
    }

    public Mono<Map> get(String endpoint, Map<String, String> headers) {
        return webClientBuilder
                .build()
                .get()
                .uri(endpoint)
                .headers(generateHttpHeaders(headers))
                .retrieve()
                .bodyToMono(Map.class);
    }

    public Mono<ClientResponse> post(String endpoint, Object request, Map<String, String> headers, int timeOut) {
        return webClientBuilder.build()
                .post()
                .uri(endpoint)
                .body(BodyInserters.fromValue(request))
                .headers(generateHttpHeaders(headers))
                .exchange()
                .onErrorResume(this::handleErrorResume)
                .timeout(Duration.ofMillis(timeOut));
    }


    public Mono<Map> post(String endpoint, Object request, Map<String, String> headers) {
        return webClientBuilder.build()
                .post()
                .uri(endpoint)
                .headers(generateHttpHeaders(headers))
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(Map.class);
    }

    private Consumer<HttpHeaders> generateHttpHeaders(Map<String, String> headers) {
        return consumerHttpHeader -> {
            consumerHttpHeader.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.forEach(consumerHttpHeader::add);
        };
    }
    public Mono<ClientResponse> post(String endpoint, Object request) {
        return webClientBuilder.build()
                .post()
                .uri("http://localhost:8081/api/v1/merchant/login")
                .body(BodyInserters.fromValue(request))
                .exchange()
                .onErrorResume(this::handleErrorResume)
                .timeout(Duration.ofMillis(DEFAULT_TIME_OUT));
    }


    private Mono<ClientResponse> throwException(Throwable err) {
        log.info("Exception {}, Message {}", err.getClass().getSimpleName(), err.getLocalizedMessage());
        return Mono.error(err);
    }


    private Mono<ClientResponse> handleErrorResume(Throwable err) {
        log.info("Exception {}, Message {}", err.getClass().getSimpleName(), err.getLocalizedMessage());
        var errorResponse = Response.builder()
                .errors(List.of(messageProvider.getServerError(),messageProvider.getAPICommError()))
                .build();
        CustomException ex = new CustomException(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        if (err instanceof TimeoutException) {
            log.info("TimeoutException {}, Message {}", err.getClass().getSimpleName(), err.getLocalizedMessage());
            ex = new CustomException(errorResponse, HttpStatus.GATEWAY_TIMEOUT);
        }
        return Mono.error(ex);
    }
}
