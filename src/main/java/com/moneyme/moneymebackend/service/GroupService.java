package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.dto.response.GroupResponseDTO;
import com.moneyme.moneymebackend.dto.response.UserResponseDTO;
import com.moneyme.moneymebackend.dto.request.CreateGroupRequest;
import com.moneyme.moneymebackend.dto.response.GroupDetailsResponse;
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

    public GroupDetailsResponse getGroupInfo(UUID groupId) {
        GroupEntity group = repository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));
        return buildGroupResponse(group);
    }

    @Transactional
    public GroupDetailsResponse createGroup(CreateGroupRequest request) {
        GroupEntity savedGroup = createAndSaveGroup(request.getGroupName());
        UserEntity user = userRepository.findById(UUID.fromString(request.getUserUuid())).orElseThrow(() -> new IllegalArgumentException("user not found"));
        user.setName(request.getHostName());

        List<UserEntity> userEntityList = new ArrayList<>();
        userEntityList.add(user);
        createUserGroup(user, savedGroup);

        request.getUsersName().forEach(userName -> {
            if (user.getName().equals(userName)) {
                return;
            }
            UserEntity newUser = createAndSaveNoAuthUser(userName);
            createUserGroup(newUser, savedGroup);
            userEntityList.add(newUser);
        });

        List<UserResponseDTO> userResponseDTOList = userEntityList.stream().map(UserResponseDTO::from).toList();
        GroupDetailsResponse response = GroupDetailsResponse.builder()
                .userResponseDTOS(userResponseDTOList)
                .groupResponseDTO(GroupResponseDTO.from(savedGroup))
                .build();

        return response;
    }

    private GroupDetailsResponse buildGroupResponse(GroupEntity group) {
        List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuid(group.getUuid());
        List<UserEntity> users = userGroups.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        List<UserResponseDTO> userResponseDTOS = users.stream().map(UserResponseDTO::from).collect(Collectors.toList());

        GroupResponseDTO groupResponse = GroupResponseDTO.from(group);
        return GroupDetailsResponse.builder()
                .userResponseDTOS(userResponseDTOS)
                .groupResponseDTO(groupResponse)
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
