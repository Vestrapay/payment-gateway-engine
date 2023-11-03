package com.example.gateway.commons.configs;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfiguration {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;


    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean("redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(){
        final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new GenericToStringSerializer<>(String.class));
        redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        return redisTemplate;
    }

    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (!redisPassword.isEmpty()) {
            redisConfiguration.setPassword(redisPassword);
        }

        SocketOptions socketOptions = SocketOptions.builder().keepAlive(true).keepAlive(SocketOptions.KeepAliveOptions.builder().interval(Duration.ofSeconds(300)).enable().build())// Enable TCP keep-alive
                .build();

        // Set the socket options in ClientOptions
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .pingBeforeActivateConnection(true)
                .build();

        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder().clientOptions(clientOptions) // Enable TCP keep-alive
                         // Set connection timeout to 10 seconds
                        .build();


        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfiguration, clientConfiguration);
        factory.afterPropertiesSet();
        return factory;
    }
}
