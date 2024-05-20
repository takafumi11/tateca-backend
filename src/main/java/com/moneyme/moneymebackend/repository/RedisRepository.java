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
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class RedisRepository {
    private final StringRedisTemplate redisTemplate;

    public void saveAmountsIntoRedis(String key, Map<String, String> updates) {
        redisTemplate.opsForHash().putAll(key, updates);
    }

    public Map<String, BigDecimal> getAmountsFromRedis(String key, List<String> hashKeys) {
        Map<String, BigDecimal> results = new HashMap<>();
        List<Object> values = redisTemplate.opsForHash().multiGet(key, (Collection<Object>) (Collection<?>) hashKeys);

        IntStream.range(0, hashKeys.size()).forEach(i -> {
            String hashKey = hashKeys.get(i);
            Object value = values.get(i);
            if (value == null) {
                if (results.containsKey(hashKey)) {
                    throw new IllegalArgumentException("Missing value for key: " + hashKey);
                }
                results.put(hashKey, BigDecimal.ZERO);
            } else {
                try {
                    BigDecimal amount = new BigDecimal(value.toString());
                    results.put(hashKey, amount);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid BigDecimal value for key: " + hashKey, e);
                }
            }
        });

        return results;
    }
}
