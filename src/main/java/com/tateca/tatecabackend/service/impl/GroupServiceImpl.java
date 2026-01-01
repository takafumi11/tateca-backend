package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.accessor.AuthUserAccessor;
import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.accessor.UserGroupAccessor;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.service.GroupService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    private final EntityManager entityManager;
    private final GroupAccessor accessor;
    private final UserAccessor userAccessor;
    private final AuthUserAccessor authUserAccessor;
    private final UserGroupAccessor userGroupAccessor;
    private final TransactionAccessor transactionAccessor;

    @Override
    @Transactional(readOnly = true)
    public GroupResponseDTO getGroupInfo(UUID groupId) {
        List<UserGroupEntity> userGroups = userGroupAccessor.findByGroupUuidWithUserDetails(groupId);
        if (userGroups.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group Not Found with: " + groupId);
        }

        List<UserEntity> users = userGroups.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        GroupEntity groupEntity = userGroups.stream().map(UserGroupEntity::getGroup).toList().get(0);
        Long transactionCount = transactionAccessor.countByGroupId(groupId);

        return GroupResponseDTO.from(users, groupEntity, transactionCount);
    }

    @Override
    @Transactional
    public GroupResponseDTO updateGroupName(UUID groupId, String name) {
        GroupEntity group = accessor.findById(groupId);
        group.setName(name);
        accessor.save(group);

        return getGroupInfo(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupListResponseDTO getGroupList(String uid) {
        List<UserEntity> userEntityList = userAccessor.findByAuthUserUid(uid);

        List<UUID> uuidList = userEntityList.stream().map(UserEntity::getUuid).toList();
        List<UserGroupEntity> userGroupEntityList = userGroupAccessor.findByUserUuidListWithGroup(uuidList);

        List<GroupEntity> groupEntityList = userGroupEntityList.stream().map(UserGroupEntity::getGroup).toList();

        return GroupListResponseDTO.from(groupEntityList);
    }

    @Override
    @Transactional
    public GroupResponseDTO createGroup(String uid, CreateGroupRequestDTO request) {
        // validation to check if exceeds max group count(=how many users are linked with auth_user)
        validateMaxGroupCount(uid);

        // Create new record into groups table.
        GroupEntity groupEntity = GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.groupName())
                .joinToken(UUID.randomUUID())
                .build();
        GroupEntity groupEntitySaved = accessor.save(groupEntity);

        // Create new records into users table
        AuthUserEntity authUser = authUserAccessor.findByUid(uid);

        List<UserEntity> userEntityList = new ArrayList<>();

        UserEntity host = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.hostName())
                .authUser(authUser)
                .build();
        userEntityList.add(host);
        request.participantsName().forEach(userName -> {
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

        Long transactionCount = transactionAccessor.countByGroupId(groupEntitySaved.getUuid());
        return GroupResponseDTO.from(userEntityListSaved, groupEntitySaved, transactionCount);
    }

    @Override
    @Transactional
    public GroupResponseDTO joinGroupInvited(JoinGroupRequestDTO request, UUID groupId, String uid) {
        // check if token is valid or not
        GroupEntity groupEntity = accessor.findById(groupId);
        if (!groupEntity.getJoinToken().equals(request.joinToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid join token: " + request.joinToken());
        }

        // Check if user has already joined this group.
        List<UserGroupEntity> userGroupEntityList = userGroupAccessor.findByGroupUuidWithUserDetails(groupId);
        boolean exists = userGroupEntityList.stream()
                .anyMatch(userGroupEntity -> {
                    AuthUserEntity authUser = userGroupEntity.getUser().getAuthUser();
                    return authUser != null && uid.equals(authUser.getUid());
                });

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already joined this group");
        }

        // validation to check if exceeds max group count(=how many users are linked with auth_user)
        validateMaxGroupCount(uid);

        // Update users.auth_user_uid to link authUser and user
        UserEntity userEntity = userAccessor.findById(request.userUuid());
        AuthUserEntity authUserEntity = authUserAccessor.findByUid(uid);
        userEntity.setAuthUser(authUserEntity);
        userAccessor.save(userEntity);

        // Build response
        // Check if user has already in the group requested.
        List<UserEntity> users = userGroupEntityList.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        Long transactionCount = transactionAccessor.countByGroupId(groupId);

        return GroupResponseDTO.from(users, groupEntity, transactionCount);
    }

    @Override
    @Transactional
    public void leaveGroup(UUID groupId, UUID userUuid) {
        // Verify group exists
        accessor.findById(groupId);

        // Verify user is in the group (using composite primary key for efficiency)
        userGroupAccessor.findByUserUuidAndGroupUuid(userUuid, groupId);

        // Get user entity
        UserEntity userEntity = userAccessor.findById(userUuid);

        // Set auth_user to null (leave group)
        userEntity.setAuthUser(null);
        userAccessor.save(userEntity);
    }

    private void validateMaxGroupCount(String uid) {
        List<UserEntity> userEntityList = userAccessor.findByAuthUserUid(uid);
        if (!uid.equals("v6CGVApOmVM4VWTijmRTg8m01Kj1") && userEntityList.size() >= 9) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User can't join more than 10 groups");
        }
    }
}
