package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.request.CreateGroupRequest;
import com.moneyme.moneymebackend.dto.request.JoinGroupRequest;
import com.moneyme.moneymebackend.dto.response.UserGroupsResponse;
import com.moneyme.moneymebackend.dto.response.GroupResponse;
import com.moneyme.moneymebackend.dto.response.UserResponse;
import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import com.moneyme.moneymebackend.entity.UserGroupEntity;
import com.moneyme.moneymebackend.repository.GroupRepository;
import com.moneyme.moneymebackend.repository.UserGroupRepository;
import com.moneyme.moneymebackend.repository.UserRepository;
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
    private final GroupRepository repository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;

    @Transactional
    public UserGroupsResponse getGroupInfo(String groupId) {
        UUID groupUuid = UUID.fromString(groupId);
        GroupEntity group = repository.findById(groupUuid).orElseThrow(() -> new IllegalArgumentException("Group not found"));
        return buildGroupResponse(group);
    }

//    @Transactional
//    public UserGroupsResponse joinGroup(String uuid, JoinGroupRequest request) {
//        UUID groupUuid = UUID.fromString(uuid);
//        GroupEntity group = repository.findById(groupUuid)
//                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
//
//        if (!group.getJoinToken().equals(request.getJoinToken())) {
//            throw new IllegalArgumentException("join token is not valid");
//        }
//
//        return buildGroupResponse(group);
//    }

    @Transactional
    public UserGroupsResponse createGroup(CreateGroupRequest request) {
        GroupEntity savedGroup = createAndSaveGroup(request.getGroupName());
        UserEntity user = userRepository.findByUuid(UUID.fromString(request.getUserUuid()));
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        List<UserEntity> userEntityList = new ArrayList<>();
        userEntityList.add(user);
        createUserGroup(user, savedGroup);

        request.getUsersName().forEach(userName -> {
            UserEntity newUser = createAndSaveNoAuthUser(userName);
            createUserGroup(newUser, savedGroup);
            userEntityList.add(newUser);
        });

        List<UserResponse> userResponseList = userEntityList.stream().map(UserResponse::from).toList();
        UserGroupsResponse response = UserGroupsResponse.builder()
                .userInfo(userResponseList)
                .groupInfo(GroupResponse.from(savedGroup))
                .build();

        return response;
    }

    private UserGroupsResponse buildGroupResponse(GroupEntity group) {
        List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuid(group.getUuid());
        List<UserEntity> users = userGroups.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        List<UserResponse> userResponses = users.stream().map(UserResponse::from).collect(Collectors.toList());

        GroupResponse groupResponse = GroupResponse.from(group);
        return UserGroupsResponse.builder()
                .userInfo(userResponses)
                .groupInfo(groupResponse)
                .build();
    }

    private GroupEntity createAndSaveGroup(String groupName) {
        GroupEntity group = GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name(groupName)
                .joinToken(UUID.randomUUID())
                .build();
        return repository.save(group);
    }

    private UserEntity createAndSaveNoAuthUser(String userName) {
        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(userName)
                .build();
        return userRepository.save(user);
    }

    private void createUserGroup(UserEntity user, GroupEntity group) {
        UserGroupEntity userGroup = UserGroupEntity.builder()
                .userUuid(user.getUuid())
                .groupUuid(group.getUuid())
                .user(user)
                .group(group)
                .build();
        userGroupRepository.save(userGroup);
    }
}
