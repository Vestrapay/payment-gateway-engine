package com.example.gateway.commons.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("gateway service")
                .packagesToScan("com.example.gateway")
                .build();
    }

    @Bean
    public OpenAPI customConfig(){
        return new OpenAPI()
                .info(new Info() .title("VestraPay Payment Gateway-Service"))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .addServersItem(new Server().url("/"));
    }
}
