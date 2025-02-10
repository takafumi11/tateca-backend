package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.repository.RedisRepository;
import io.lettuce.core.RedisException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisRepository repository;

    public Map<String, BigDecimal> getBalances(String groupId, List<String> userIds) {
        try {
            return repository.getAmountsFromRedis(groupId, userIds);
        } catch (Exception e) {
            throw new RedisException("Failed to get data from redis", e);
        }
    }

    @Transactional
    public void updateBalances(String groupId, Map<String, BigDecimal> userAmounts) {
        try {
            // 既存の値を取得
            Map<String, BigDecimal> currentBalances = repository.getAmountsFromRedis(groupId, new ArrayList<>(userAmounts.keySet()));

            // 新しい値を加算
            Map<String, String> updates = new HashMap<>();
            userAmounts.forEach((userId, amountToAdd) -> {
                BigDecimal currentAmount = currentBalances.getOrDefault(userId, BigDecimal.ZERO);
                BigDecimal newAmount = currentAmount.add(amountToAdd);
                updates.put(userId, newAmount.toString());
            });

            // 更新された値をRedisに保存
            repository.saveAmountsIntoRedis(groupId, updates);
        } catch (Exception e) {
            throw new RedisException("Failed to save data into redis", e);
        }
    }


}
