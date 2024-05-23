package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.accessor.GroupAccessor;
import com.moneyme.moneymebackend.accessor.UserAccessor;
import com.moneyme.moneymebackend.accessor.UserGroupAccessor;
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
    private final GroupAccessor accessor;
    private final UserRepository userRepository;
    private final UserAccessor userAccessor;
    private final UserGroupAccessor userGroupAccessor;
    private final UserGroupRepository userGroupRepository;

    public GroupDetailsResponse getGroupInfo(UUID groupId) {
        List<UserGroupEntity> userGroups = userGroupAccessor.findByGroupUuid(groupId);
        List<UserEntity> users = userGroups.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        GroupEntity groupEntity = userGroups.stream().map(UserGroupEntity::getGroup).toList().get(0);

        return GroupDetailsResponse.from(users, groupEntity);
    }

    @Transactional
    public GroupDetailsResponse createGroup(CreateGroupRequest request) {
        GroupEntity savedGroup = createAndSaveGroup(request.getGroupName());
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
            UserEntity noAuthUserSaved = userRepository.save(noAuthUser);
            createUserGroup(noAuthUserSaved, savedGroup);
            userEntityList.add(noAuthUserSaved);
        });

        return GroupDetailsResponse.from(userEntityList, savedGroup);
    }

    private GroupEntity createAndSaveGroup(String groupName) {
        GroupEntity group = GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name(groupName)
                .joinToken(UUID.randomUUID())
                .build();
        return repository.save(group);
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
