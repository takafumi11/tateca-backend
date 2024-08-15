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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
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

    public GroupDetailsResponse getGroupInfo(String uid, UUID groupId) {
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

//        String userUuid = users.stream().map(UserEntity::getAuthUser).filter(authUser -> {
//            return authUser.getUid().equals(uid);
//        }).toString();

        return GroupDetailsResponse.from(users, groupEntity);
    }

    public GetGroupListResponse getGroupList(String uid) {
        List<UserEntity> userEntityList = userAccessor.findByAuthUserUid(uid);

        List<UUID> uuidList = userEntityList.stream().map(UserEntity::getUuid).toList();
        List<UserGroupEntity> userGroupEntityList = userGroupAccessor.findByUserUuidList(uuidList);

        List<GroupEntity> groupEntityList = userGroupEntityList.stream().map(UserGroupEntity::getGroup).toList();

        if (groupEntityList.size() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found");
        }

        return GetGroupListResponse.from(groupEntityList);
    }

    @Transactional
    public GroupDetailsResponse createGroup(String uid, CreateGroupRequest request) {
        // validation to check if exceeds max group count(=how many users are linked with auth_user)
        validateMaxGroupCount(uid);

        // Create new record into groups table.
        GroupEntity groupEntity = GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.getGroupName())
                .joinToken(UUID.randomUUID())
                .build();
        GroupEntity groupEntitySaved = accessor.save(groupEntity);

        // Create new records into users table
        AuthUserEntity authUser = authUserAccessor.findByUid(uid);
        List<UserEntity> userEntityList = new ArrayList<>();

        UserEntity host = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.getHostName())
                .authUser(authUser)
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

        return GroupDetailsResponse.from(userEntityListSaved, groupEntitySaved);
    }

    @Transactional
    public GroupDetailsResponse joinGroup(JoinGroupRequest request, UUID groupId, String uid) {
        // check if token is valid or not
        GroupEntity groupEntity = accessor.findById(groupId);
        if (!groupEntity.getJoinToken().equals(request.getJoinToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid join token: " + request.getJoinToken());
        }

        // validation to check if exceeds max group count(=how many users are linked with auth_user)
        validateMaxGroupCount(uid);

        // Update users.auth_user_uid to link authUser and user
        UserEntity userEntity = userAccessor.findById(request.getUserUuid());
        AuthUserEntity authUserEntity = authUserAccessor.findByUid(uid);
        userEntity.setAuthUser(authUserEntity);
        userAccessor.save(userEntity);

        // Build response
        List<UserGroupEntity> updatedUserGroupList = userGroupAccessor.findByGroupUuid(groupId);
        List<UserEntity> users = updatedUserGroupList.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());

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

    @Transactional
    private void validateMaxGroupCount(String uid) {
        try {
            List<UserEntity> userEntityList = userAccessor.findByAuthUserUid(uid);
            if (userEntityList.size() >= 5) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User can't join more than 6 groups");
            }
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                // Do nothing
            } else {
                throw e;
            }
        }
    }
}
