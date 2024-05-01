package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisRepository repository;

    private static final String KEY_TEMPLATE = "user-{userUuid}-transactions-in-{groupUuid}";

    /**
     * Updates the balances in Redis for two users involved in a transaction.
     * User1 lends money to User2, hence the balance for User2 (as hashKey) is incremented by the amount.
     * Conversely, User2 owes money to User1, so the balance for User1 (as hashKey) is decremented by the amount.
     *
     * @param user1   the UUID of the user who lends money
     * @param user2   the UUID of the user who borrows money
     * @param amount  the amount of money involved in the transaction
     * @param groupId the group UUID within which this transaction occurs
     */
    public void updateBalances(String user1, String user2, BigDecimal amount, String groupId) {
        String key1 = generateKey(user1, groupId);
        String key2 = generateKey(user2, groupId);

        BigDecimal currentAmount1 = repository.getAmountFromRedis(key1, user2);
        BigDecimal currentAmount2 = repository.getAmountFromRedis(key2, user1);

        repository.saveAmountIntoRedis(key1, user2, currentAmount1.add(amount).toString());
        repository.saveAmountIntoRedis(key2, user1, currentAmount2.add(amount.negate()).toString());
    }

    private String generateKey(String userUuid, String groupUuid) {
        return KEY_TEMPLATE.replace("{userUuid}", userUuid).replace("{groupUuid}", groupUuid);
    }
}
