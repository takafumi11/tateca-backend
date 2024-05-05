package com.moneyme.moneymebackend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RedisRepository {
    private final StringRedisTemplate redisTemplate;

    public void saveAmountsIntoRedis(String key, Map<String, String> updates) {
        redisTemplate.opsForHash().putAll(key, updates);
    }

    public BigDecimal getAmountFromRedis(String key, String hashKey) {
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        return (value != null) ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
    }

    public Map<String, BigDecimal> getAmountsFromRedis(String key, List<String> hashKeys) {
        Map<String, BigDecimal> results = new HashMap<>();
        // Cast List<String> to Collection<Object> explicitly
        List<Object> values = redisTemplate.opsForHash().multiGet(key, (Collection<Object>) (Collection<?>) hashKeys);

        for (int i = 0; i < hashKeys.size(); i++) {
            String hashKey = hashKeys.get(i);
            Object value = values.get(i);
            BigDecimal amount = (value != null) ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
            results.put(hashKey, amount);
        }
        return results;
    }
}
