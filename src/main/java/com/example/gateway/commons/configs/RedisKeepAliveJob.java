package com.example.gateway.commons.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisKeepAliveJob {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisKeepAliveJob(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 120000)
    public void pingRedis() {
        log.info("Pinging redis");
        String pingResponse  = redisTemplate.getConnectionFactory().getConnection().ping();
        log.info("Done pinging redis: {}", pingResponse);
    }
}