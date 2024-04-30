package com.moneyme.moneymebackend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class RedisRepository {
    private final StringRedisTemplate redisTemplate;

    public void saveAmountIntoRedis(String key, String hashKey, String value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public BigDecimal getAmountFromRedis(String key, String hashKey) {
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        return (value != null) ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
    }
}
