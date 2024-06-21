package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.accessor.GroupAccessor;
import com.moneyme.moneymebackend.accessor.LoanAccessor;
import com.moneyme.moneymebackend.accessor.ObligationAccessor;
import com.moneyme.moneymebackend.accessor.RepaymentAccessor;
import com.moneyme.moneymebackend.accessor.UserAccessor;
import com.moneyme.moneymebackend.accessor.UserGroupAccessor;
import com.moneyme.moneymebackend.dto.request.CreateGroupRequest;
import com.moneyme.moneymebackend.dto.request.JoinGroupRequest;
import com.moneyme.moneymebackend.dto.response.GetGroupListResponse;
import com.moneyme.moneymebackend.dto.response.GroupDetailsResponse;
import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.ObligationEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.entity.UserGroupEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupAccessor accessor;
    private final UserAccessor userAccessor;
    private final UserGroupAccessor userGroupAccessor;
    private final RepaymentAccessor repaymentAccessor;
    private final LoanAccessor loanAccessor;
    private final ObligationAccessor obligationAccessor;

    public GroupDetailsResponse getGroupInfo(UUID groupId) {
        List<UserGroupEntity> userGroups = userGroupAccessor.findByGroupUuid(groupId);
        List<UserEntity> users = userGroups.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        GroupEntity groupEntity = userGroups.stream().map(UserGroupEntity::getGroup).toList().get(0);

        return GroupDetailsResponse.from(users, groupEntity);
    }

    public GetGroupListResponse getGroupList(UUID userId) {
        List<UserGroupEntity> userGroups = userGroupAccessor.findByUserUuid(userId);

        List<GroupEntity> groupEntityList = userGroups.stream().map(UserGroupEntity::getGroup).toList();

        return GetGroupListResponse.from(groupEntityList);
    }

    @Transactional
    public GroupDetailsResponse createGroup(CreateGroupRequest request) {
        GroupEntity group = GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.getGroupName())
                .joinToken(UUID.randomUUID())
                .build();
        GroupEntity savedGroup = accessor.save(group);

        UserEntity user = userAccessor.findById(UUID.fromString(request.getHostUuid()));

        List<UserEntity> userEntityList = new ArrayList<>();
        userEntityList.add(user);
        createUserGroup(user, savedGroup);

        request.getParticipantsName().forEach(userName -> {
            if(userName.equals(user.getName())) {
                return;
            }
            UserEntity noAuthUser = UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name(userName)
                    .build();
            UserEntity noAuthUserSaved = userAccessor.save(noAuthUser);
            createUserGroup(noAuthUserSaved, savedGroup);
            userEntityList.add(noAuthUserSaved);
        });

        return GroupDetailsResponse.from(userEntityList, savedGroup);
    }

    @Transactional
    public GroupDetailsResponse joinGroup(JoinGroupRequest request, UUID groupId, String token) {
        UUID tmpUserId = UUID.fromString(request.getTmpUserId());
        UUID actualUserId = UUID.fromString(request.getActualUserId());

        userGroupAccessor.delete(tmpUserId, groupId);

        GroupEntity group = accessor.findById(groupId);
        UserEntity actualUser = userAccessor.findById(actualUserId);
        createUserGroup(actualUser, group);

        // repayment, loanの修正が必要
        List<RepaymentEntity> repaymentList = repaymentAccessor.findByGroupId(groupId);
        for (RepaymentEntity repayment : repaymentList) {
            if (repayment.getPayer().getUuid().equals(tmpUserId)) {
                repayment.setPayer(actualUser);
            }

            if (repayment.getRecipientUser().getUuid().equals(tmpUserId)) {
                repayment.setRecipientUser(actualUser);
            }

            repaymentAccessor.save(repayment);
        }

        List<ObligationEntity> obligationList = obligationAccessor.findByGroupId(groupId);
        for (ObligationEntity obligation : obligationList) {
            if (obligation.getUser().getUuid().equals(tmpUserId)) {
                obligation.setUser(actualUser);
            }
            obligationAccessor.save(obligation);
        }

        List<LoanEntity> loanList = loanAccessor.findByGroupId(groupId);
        for (LoanEntity loan : loanList) {
            if (loan.getPayer().getUuid().equals(tmpUserId)) {
                loan.setPayer(actualUser);
            }

            loanAccessor.save(loan);
        }

        List<UserGroupEntity> updatedUserGroupList = userGroupAccessor.findByGroupUuid(groupId);

        List<UserEntity> users = updatedUserGroupList.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        GroupEntity groupEntity = updatedUserGroupList.stream().map(UserGroupEntity::getGroup).toList().get(0);

        return GroupDetailsResponse.from(users, groupEntity);
    }

    private void createUserGroup(UserEntity user, GroupEntity group) {
        UserGroupEntity userGroup = UserGroupEntity.builder()
                .userUuid(user.getUuid())
                .groupUuid(group.getUuid())
                .user(user)
                .group(group)
                .build();
        userGroupAccessor.save(userGroup);
    }
}
