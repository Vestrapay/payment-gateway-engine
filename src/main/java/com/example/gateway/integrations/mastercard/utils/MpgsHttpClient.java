package com.example.gateway.integrations.mastercard.utils;

import com.example.gateway.commons.exceptions.CustomException;
import com.example.gateway.commons.utils.MessageProvider;
import com.example.gateway.commons.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpgsHttpClient {
    @Value("${mpgs.baseUrl}")
    private String baseUrl;
    @Value("${mpgs.merchantId}")
    private String merchantId;
    @Value("${mpgs.apiPassword}")
    private String apiPassword;

    private final WebClient.Builder webClientBuilder;
    private static final Long DEFAULT_TIME_OUT = 30000L;
    private final MessageProvider messageProvider;


    public Mono<ClientResponse> post(String path, Object request) {
        String endpoint = baseUrl.concat(path);
        return webClientBuilder.build()
                .post()
                .uri(endpoint)
                .headers(generateHttpHeaders())
                .body(BodyInserters.fromValue(request))
                .exchange()
                .onErrorResume(this::handleErrorResume);
    }

    public Mono<ClientResponse> put(String path, Object request) {
        String endpoint = baseUrl.concat(path);
        log.info(endpoint);
        return webClientBuilder.build()
                .put()
                .uri(endpoint)
                .headers(generateHttpHeaders())
                .body(BodyInserters.fromValue(request))
                .exchange()
                .onErrorResume(this::handleErrorResume);
    }

    public Mono<ClientResponse> get(String path) {
        String endpoint = baseUrl.concat(path);
        return webClientBuilder.build()
                .get()
                .uri(endpoint)
                .headers(generateHttpHeaders())
                .exchange()
                .onErrorResume(this::handleErrorResume);
    }

    private Consumer<HttpHeaders> generateHttpHeaders() {
        Map<String,String>headers = new HashMap<>();
        String auth = "merchant."+merchantId+":"+apiPassword;
        String token = "Basic "+Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.put("Authorization",token);

        return consumerHttpHeader -> {
            consumerHttpHeader.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.forEach(consumerHttpHeader::add);
        };
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
