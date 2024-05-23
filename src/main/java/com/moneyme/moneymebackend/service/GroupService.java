package com.moneyme.moneymebackend.service;

import com.moneyme.moneymebackend.accessor.GroupAccessor;
import com.moneyme.moneymebackend.accessor.UserAccessor;
import com.moneyme.moneymebackend.accessor.UserGroupAccessor;
import com.moneyme.moneymebackend.dto.request.CreateGroupRequest;
import com.moneyme.moneymebackend.dto.response.GroupDetailsResponse;
import com.moneyme.moneymebackend.entity.GroupEntity;
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

    public GroupDetailsResponse getGroupInfo(UUID groupId) {
        List<UserGroupEntity> userGroups = userGroupAccessor.findByGroupUuid(groupId);
        List<UserEntity> users = userGroups.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        GroupEntity groupEntity = userGroups.stream().map(UserGroupEntity::getGroup).toList().get(0);

        return GroupDetailsResponse.from(users, groupEntity);
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
