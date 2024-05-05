package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.model.UserBalanceResponseModel;
import com.moneyme.moneymebackend.dto.response.UserBalanceResponse;
import com.moneyme.moneymebackend.entity.UserGroupEntity;
import com.moneyme.moneymebackend.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserBalanceService {
    private final RedisService service;

    private final UserGroupRepository userGroupRepository;

    public UserBalanceResponse getUserBalances(String groupId) {
        List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuid(UUID.fromString(groupId));
        List<UUID> userIds = userGroups.stream().map(UserGroupEntity::getUserUuid).toList();
        List<UserBalanceResponseModel> balances = new ArrayList<>();

//        for (int i = 0; i < userIds.size(); i++) {
//            for (int j = i + 1; j < userIds.size(); j++) {
//                String user1 = userIds.get(i).toString();
//                String user2 = userIds.get(j).toString();
//                UserBalanceResponseModel balance = service.getBalanceBetweenUsers(user1, user2, groupId);
//                balances.add(balance);
//            }
//        }

        return UserBalanceResponse.builder()
                .balances(balances)
                .build();
    }
}
