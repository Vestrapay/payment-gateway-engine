package com.example.gateway.commons.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisUtility {
    @Qualifier("redisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    private final String defaultKey = "Wallet:";

    public void setValue(String key, Object value){
        redisTemplate.opsForValue().set(defaultKey + key, value);
    }

    public void setValue(String key, Object value, long expiry){
        redisTemplate.opsForValue().set(defaultKey + key, value, Duration.ofSeconds(expiry));
    }

    public Object getValue(String key){
        return redisTemplate.opsForValue().get(defaultKey + key);
    }

    public void removeValue(String key){
        redisTemplate.delete(defaultKey + key);
    }

}
