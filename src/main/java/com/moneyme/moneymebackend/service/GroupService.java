package com.moneyme.moneymebackend.service;

import com.google.api.Http;
import com.moneyme.moneymebackend.accessor.AuthUserAccessor;
import com.moneyme.moneymebackend.accessor.GroupAccessor;
import com.moneyme.moneymebackend.accessor.LoanAccessor;
import com.moneyme.moneymebackend.accessor.ObligationAccessor;
import com.moneyme.moneymebackend.accessor.RepaymentAccessor;
import com.moneyme.moneymebackend.accessor.UserAccessor;
import com.moneyme.moneymebackend.accessor.UserAuthUserAccessor;
import com.moneyme.moneymebackend.accessor.UserGroupAccessor;
import com.moneyme.moneymebackend.dto.request.CreateGroupRequest;
import com.moneyme.moneymebackend.dto.request.JoinGroupRequest;
import com.moneyme.moneymebackend.dto.response.GetGroupListResponse;
import com.moneyme.moneymebackend.dto.response.GroupDetailsResponse;
import com.moneyme.moneymebackend.entity.AuthUserEntity;
import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.LoanEntity;
import com.moneyme.moneymebackend.entity.ObligationEntity;
import com.moneyme.moneymebackend.entity.RepaymentEntity;
import com.moneyme.moneymebackend.entity.UserAuthUserEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.entity.UserGroupEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupAccessor accessor;
    private final UserAccessor userAccessor;
    private final AuthUserAccessor authUserAccessor;
    private final UserGroupAccessor userGroupAccessor;
    private final UserAuthUserAccessor userAuthUserAccessor;
    private final RepaymentAccessor repaymentAccessor;
    private final LoanAccessor loanAccessor;
    private final ObligationAccessor obligationAccessor;

    public GroupDetailsResponse getGroupInfo(UUID groupId) {
//        Integer inta = 10;
//
//        if (inta == 10) {
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "失敗！");
//        }

        List<UserGroupEntity> userGroups = userGroupAccessor.findByGroupUuid(groupId);
        if (userGroups.size() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found with: " + groupId);
        }

        List<UserEntity> users = userGroups.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        GroupEntity groupEntity = userGroups.stream().map(UserGroupEntity::getGroup).toList().get(0);

        return GroupDetailsResponse.from(users, groupEntity, "");
    }

    public GetGroupListResponse getGroupList(String uid) {
        AuthUserEntity authUserEntity = authUserAccessor.findByUid(uid);
        List<UserAuthUserEntity> userAuthUserEntityList = userAuthUserAccessor.findByAuthUserUuid(authUserEntity.getUuid());

        List<GroupEntity> groupEntityList = new ArrayList<>();
        userAuthUserEntityList.forEach(userAuthUserEntity -> {
            UserGroupEntity userGroupEntity = userGroupAccessor.findByUserUuid(userAuthUserEntity.getUserUuid()).get(0);
            groupEntityList.add(userGroupEntity.getGroup());
        });

        if (groupEntityList.size() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found");
        }

        return GetGroupListResponse.from(groupEntityList);
    }

    @Transactional
    public GroupDetailsResponse createGroup(String uid, CreateGroupRequest request) {
        // validation to check if exceeds max group count(=how many users are linked with auth_user)
        AuthUserEntity authUser = authUserAccessor.findByUid(uid);
        List<UserAuthUserEntity> userAuthUserEntityList = userAuthUserAccessor.findByAuthUserUuid(authUser.getUuid());
        if (userAuthUserEntityList.size() >= 5) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User can't join more than 6 groups");
        }

        // Create new records into users table
        List<UserEntity> userEntityList = new ArrayList<>();

        UserEntity host = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.getHostName())
                .build();
        userEntityList.add(host);

        request.getParticipantsName().forEach(userName -> {
            UserEntity user = UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name(userName)
                    .build();
            userEntityList.add(user);
        });
        List<UserEntity> userEntityListSaved = userAccessor.saveAll(userEntityList);

        // Create new record into groups table.
        GroupEntity groupEntity = GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.getGroupName())
                .joinToken(UUID.randomUUID())
                .build();
        GroupEntity groupEntitySaved = accessor.save(groupEntity);

        // Create new records into user_groups table
        List<UserGroupEntity> userGroupEntityList = new ArrayList<>();
        userEntityListSaved.forEach(userEntity -> {
            UserGroupEntity userGroupEntity = UserGroupEntity.builder()
                    .userUuid(userEntity.getUuid())
                    .groupUuid(groupEntitySaved.getUuid())
                    .user(userEntity)
                    .group(groupEntitySaved)
                    .build();
            userGroupEntityList.add(userGroupEntity);
        });
        userGroupAccessor.saveAll(userGroupEntityList);

        // Link host in auth_user table with host in user table
        UserAuthUserEntity userAuthUserEntity = UserAuthUserEntity.builder()
                .authUserUuid(authUser.getUuid())
                .userUuid(host.getUuid())
                .authUser(authUser)
                .user(host)
                .build();
        userAuthUserAccessor.save(userAuthUserEntity);

        return GroupDetailsResponse.from(userEntityListSaved, groupEntitySaved, host.getUuid().toString());
    }

//    @Transactional
//    public GroupDetailsResponse joinGroup(JoinGroupRequest request, UUID groupId, String token) {
//        List<UserGroupEntity> userGroupEntityList2 = userGroupAccessor.findByUserUuid(UUID.fromString(request.getActualUserId()));
//        if (userGroupEntityList2.size() >= 5) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "User can't join more than 5 groups");
//        }
//
//        List<UserGroupEntity> userGroupEntityList = userGroupAccessor.findByGroupUuid(groupId);
//
//        long emptyUserEntityCount = userGroupEntityList.stream()
//                .filter(userGroupEntity -> userGroupEntity.getUser().getUid() == null)
//                .count();
//
//        if (emptyUserEntityCount == 0) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "The group is full");
//        }
//
//        UUID tmpUserId = UUID.fromString(request.getTmpUserId());
//        UUID actualUserId = UUID.fromString(request.getActualUserId());
//
//        userGroupAccessor.delete(tmpUserId, groupId);
//
//        GroupEntity group = accessor.findById(groupId);
//        UserEntity actualUser = userAccessor.findById(actualUserId);
//        createUserGroup(actualUser, group);
//
//        // repayment, loanの修正が必要
//        List<RepaymentEntity> repaymentList = repaymentAccessor.findByGroupId(groupId);
//        for (RepaymentEntity repayment : repaymentList) {
//            if (repayment.getPayer().getUuid().equals(tmpUserId)) {
//                repayment.setPayer(actualUser);
//            }
//
//            if (repayment.getRecipientUser().getUuid().equals(tmpUserId)) {
//                repayment.setRecipientUser(actualUser);
//            }
//
//            repaymentAccessor.save(repayment);
//        }
//
//        List<ObligationEntity> obligationList = obligationAccessor.findByGroupId(groupId);
//        for (ObligationEntity obligation : obligationList) {
//            if (obligation.getUser().getUuid().equals(tmpUserId)) {
//                obligation.setUser(actualUser);
//            }
//            obligationAccessor.save(obligation);
//        }
//
//        List<LoanEntity> loanList = loanAccessor.findByGroupId(groupId);
//        for (LoanEntity loan : loanList) {
//            if (loan.getPayer().getUuid().equals(tmpUserId)) {
//                loan.setPayer(actualUser);
//            }
//
//            loanAccessor.save(loan);
//        }
//
//        List<UserGroupEntity> updatedUserGroupList = userGroupAccessor.findByGroupUuid(groupId);
//
//        List<UserEntity> users = updatedUserGroupList.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
//        GroupEntity groupEntity = updatedUserGroupList.stream().map(UserGroupEntity::getGroup).toList().get(0);
//
//        return GroupDetailsResponse.from(users, groupEntity);
//    }

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
