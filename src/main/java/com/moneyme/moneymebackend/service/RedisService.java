package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.repository.RedisRepository;
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
        return repository.getAmountsFromRedis(groupId, userIds);
    }

    public void updateBalances(String groupId, Map<String, BigDecimal> userAmounts) {
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
    }


}
