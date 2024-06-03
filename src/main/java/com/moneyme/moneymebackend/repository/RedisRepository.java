package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.exception.RedisOperationException;
import io.lettuce.core.RedisException;
import jakarta.transaction.Transactional;
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

    @Transactional
    public void saveAmountsIntoRedis(String key, Map<String, String> updates) {
        try {
            redisTemplate.opsForHash().putAll(key, updates);
        } catch (Exception e) {
            throw new RedisException("Failed to save data into redis", e);
        }
    }

    public Map<String, BigDecimal> getAmountsFromRedis(String key, List<String> hashKeys) {
        Map<String, BigDecimal> results = new HashMap<>();
        try {
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
        } catch (Exception e) {
            throw new RedisException("Failed to get data from redis", e);
        }

        return results;
    }
}
