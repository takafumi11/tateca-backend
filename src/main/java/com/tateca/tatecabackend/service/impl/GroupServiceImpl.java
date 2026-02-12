package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.config.BusinessRuleConfig;
import com.tateca.tatecabackend.constants.BusinessConstants;
import com.tateca.tatecabackend.repository.TransactionRepository;
import com.tateca.tatecabackend.dto.request.AddMemberRequestDTO;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.exception.ErrorCode;
import com.tateca.tatecabackend.exception.domain.BusinessRuleViolationException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.exception.domain.ForbiddenException;
import com.tateca.tatecabackend.repository.AuthUserRepository;
import com.tateca.tatecabackend.repository.GroupRepository;
import com.tateca.tatecabackend.repository.UserGroupRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import com.tateca.tatecabackend.service.GroupService;
import com.tateca.tatecabackend.util.PiiMaskingUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);
    private final EntityManager entityManager;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final AuthUserRepository authUserRepository;
    private final UserGroupRepository userGroupRepository;
    private final TransactionRepository transactionRepository;
    private final BusinessRuleConfig businessRuleConfig;

    @Override
    @Transactional(readOnly = true)
    public GroupResponseDTO getGroupInfo(UUID groupId) {
        List<UserGroupEntity> userGroups = userGroupRepository.findByGroupUuidWithUserDetails(groupId);
        if (userGroups.isEmpty()) {
            throw new EntityNotFoundException(ErrorCode.GROUP_NOT_FOUND);
        }

        List<UserEntity> users = userGroups.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        GroupEntity groupEntity = userGroups.stream().map(UserGroupEntity::getGroup).toList().get(0);
        Long transactionCount = transactionRepository.countByGroup_Uuid(groupId);

        return GroupResponseDTO.from(users, groupEntity, transactionCount);
    }

    @Override
    @Transactional
    public GroupResponseDTO updateGroupName(UUID groupId, String name) {
        logger.info("Updating group name: groupId={}", PiiMaskingUtil.maskUuid(groupId));

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    logger.warn("Group not found: groupId={}", PiiMaskingUtil.maskUuid(groupId));
                    return new EntityNotFoundException(ErrorCode.GROUP_NOT_FOUND);
                });
        String oldName = group.getName();
        group.setName(name);
        groupRepository.save(group);

        logger.info("Group name updated successfully: groupId={}, oldName={}, newName={}",
                PiiMaskingUtil.maskUuid(groupId), oldName, name);

        return getGroupInfo(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupListResponseDTO getGroupList(String uid) {
        List<UserEntity> userEntityList = userRepository.findByAuthUserUid(uid);

        List<UUID> uuidList = userEntityList.stream().map(UserEntity::getUuid).toList();
        List<UserGroupEntity> userGroupEntityList = userGroupRepository.findByUserUuidListWithGroup(uuidList);

        List<GroupEntity> groupEntityList = userGroupEntityList.stream().map(UserGroupEntity::getGroup).toList();

        return GroupListResponseDTO.from(groupEntityList);
    }

    @Override
    @Transactional
    public GroupResponseDTO createGroup(String uid, CreateGroupRequestDTO request) {
        int totalMembers = 1 + request.memberNames().size(); // creator + other members
        logger.info("Creating new group: userId={}, groupName={}, memberCount={}",
                PiiMaskingUtil.maskUid(uid), request.groupName(), totalMembers);

        // validation to check if exceeds max group count(=how many users are linked with auth_user)
        validateMaxGroupCount(uid);

        // Create new record into groups table.
        GroupEntity groupEntity = GroupEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.groupName())
                .joinToken(UUID.randomUUID())
                .build();
        GroupEntity groupEntitySaved = groupRepository.save(groupEntity);

        // Create new records into users table
        AuthUserEntity authUser = authUserRepository.findById(uid)
                .orElseThrow(() -> {
                    logger.warn("Auth user not found: uid={}", PiiMaskingUtil.maskUid(uid));
                    return new EntityNotFoundException(ErrorCode.AUTH_USER_NOT_FOUND);
                });

        List<UserEntity> userEntityList = new ArrayList<>();

        UserEntity creator = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.yourName())
                .authUser(authUser)
                .build();
        userEntityList.add(creator);
        request.memberNames().forEach(userName -> {
            UserEntity user = UserEntity.builder()
                    .uuid(UUID.randomUUID())
                    .name(userName)
                    .build();
            userEntityList.add(user);
        });
        List<UserEntity> userEntityListSaved = userRepository.saveAll(userEntityList);

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
        userGroupRepository.saveAll(userGroupEntityList);

        logger.info("Group created successfully: groupId={}, userId={}, memberCount={}",
                PiiMaskingUtil.maskUuid(groupEntitySaved.getUuid()),
                PiiMaskingUtil.maskUid(uid),
                totalMembers);

        Long transactionCount = transactionRepository.countByGroup_Uuid(groupEntitySaved.getUuid());
        return GroupResponseDTO.from(userEntityListSaved, groupEntitySaved, transactionCount);
    }

    @Override
    @Transactional
    public GroupResponseDTO joinGroupInvited(JoinGroupRequestDTO request, UUID groupId, String uid) {
        logger.info("User attempting to join group: userId={}, groupId={}, userUuid={}",
                PiiMaskingUtil.maskUid(uid),
                PiiMaskingUtil.maskUuid(groupId),
                PiiMaskingUtil.maskUuid(request.userUuid()));

        // check if token is valid or not
        GroupEntity groupEntity = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    logger.warn("Group not found: groupId={}", PiiMaskingUtil.maskUuid(groupId));
                    return new EntityNotFoundException(ErrorCode.GROUP_NOT_FOUND);
                });

        // Check if user has already joined this group.
        List<UserGroupEntity> userGroupEntityList = userGroupRepository.findByGroupUuidWithUserDetails(groupId);
        boolean exists = userGroupEntityList.stream()
                .anyMatch(userGroupEntity -> {
                    AuthUserEntity authUser = userGroupEntity.getUser().getAuthUser();
                    return authUser != null && uid.equals(authUser.getUid());
                });

        if (exists) {
            logger.warn("User already joined this group: userId={}, groupId={}",
                    PiiMaskingUtil.maskUid(uid), PiiMaskingUtil.maskUuid(groupId));
            throw new BusinessRuleViolationException(ErrorCode.GROUP_ALREADY_JOINED);
        }

        // validation to check if exceeds max group count(=how many users are linked with auth_user)
        validateMaxGroupCount(uid);

        if (!groupEntity.getJoinToken().equals(request.joinToken())) {
            logger.warn("Invalid join token provided: userId={}, groupId={}, tokenProvided={}",
                    PiiMaskingUtil.maskUid(uid),
                    PiiMaskingUtil.maskUuid(groupId),
                    PiiMaskingUtil.maskToken(request.joinToken().toString()));
            throw new ForbiddenException(ErrorCode.GROUP_INVALID_JOIN_TOKEN);
        }

        // Update users.auth_user_uid to link authUser and user
        UserEntity userEntity = userRepository.findById(request.userUuid())
                .orElseThrow(() -> {
                    logger.warn("User not found: userUuid={}", PiiMaskingUtil.maskUuid(request.userUuid()));
                    return new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
                });
        AuthUserEntity authUserEntity = authUserRepository.findById(uid)
                .orElseThrow(() -> {
                    logger.warn("Auth user not found: uid={}", PiiMaskingUtil.maskUid(uid));
                    return new EntityNotFoundException(ErrorCode.AUTH_USER_NOT_FOUND);
                });
        userEntity.setAuthUser(authUserEntity);
        userRepository.save(userEntity);

        logger.info("User successfully joined group: userId={}, groupId={}, groupName={}",
                PiiMaskingUtil.maskUid(uid),
                PiiMaskingUtil.maskUuid(groupId),
                groupEntity.getName());

        // Build response
        // Check if user has already in the group requested.
        List<UserEntity> users = userGroupEntityList.stream().map(UserGroupEntity::getUser).collect(Collectors.toList());
        Long transactionCount = transactionRepository.countByGroup_Uuid(groupId);

        return GroupResponseDTO.from(users, groupEntity, transactionCount);
    }

    @Override
    @Transactional
    public void leaveGroup(UUID groupId, UUID userUuid) {
        logger.info("User attempting to leave group: groupId={}, userUuid={}",
                PiiMaskingUtil.maskUuid(groupId), PiiMaskingUtil.maskUuid(userUuid));

        // Verify group exists
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    logger.warn("Group not found: groupId={}", PiiMaskingUtil.maskUuid(groupId));
                    return new EntityNotFoundException(ErrorCode.GROUP_NOT_FOUND);
                });

        // Verify user is in the group (using composite primary key for efficiency)
        userGroupRepository.findByUserUuidAndGroupUuid(userUuid, groupId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_IN_GROUP));

        // Get user entity
        UserEntity userEntity = userRepository.findById(userUuid)
                .orElseThrow(() -> {
                    logger.warn("User not found: userUuid={}", PiiMaskingUtil.maskUuid(userUuid));
                    return new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
                });

        // Set auth_user to null (leave group)
        String authUserId = userEntity.getAuthUser() != null ? userEntity.getAuthUser().getUid() : null;
        userEntity.setAuthUser(null);
        userRepository.save(userEntity);

        logger.info("User successfully left group: userId={}, groupId={}, groupName={}",
                authUserId != null ? PiiMaskingUtil.maskUid(authUserId) : "unknown",
                PiiMaskingUtil.maskUuid(groupId),
                group.getName());
    }

    @Override
    @Transactional
    public GroupResponseDTO addMember(UUID groupId, String uid, AddMemberRequestDTO request) {
        logger.info("Adding member to group: groupId={}, uid={}, memberName={}",
                PiiMaskingUtil.maskUuid(groupId),
                PiiMaskingUtil.maskUid(uid),
                request.memberName());

        // Get group members (also verifies group exists implicitly)
        List<UserGroupEntity> userGroupEntityList = userGroupRepository.findByGroupUuidWithUserDetails(groupId);

        if (userGroupEntityList.isEmpty()) {
            logger.warn("Group not found or has no members: groupId={}", PiiMaskingUtil.maskUuid(groupId));
            throw new EntityNotFoundException(ErrorCode.GROUP_NOT_FOUND);
        }

        // Verify requester is a group member (authorization check)
        boolean isRequesterMember = userGroupEntityList.stream()
                .anyMatch(userGroupEntity -> {
                    AuthUserEntity authUser = userGroupEntity.getUser().getAuthUser();
                    return authUser != null && uid.equals(authUser.getUid());
                });

        if (!isRequesterMember) {
            logger.warn("User is not a member of this group: uid={}, groupId={}",
                    PiiMaskingUtil.maskUid(uid),
                    PiiMaskingUtil.maskUuid(groupId));
            throw new ForbiddenException(ErrorCode.USER_NOT_GROUP_MEMBER);
        }

        // Check if group has reached maximum size
        int currentMemberCount = userGroupEntityList.size();
        if (currentMemberCount >= BusinessConstants.MAX_GROUP_SIZE) {
            logger.warn("Group has reached maximum size: groupId={}, currentSize={}, maxSize={}",
                    PiiMaskingUtil.maskUuid(groupId),
                    currentMemberCount,
                    BusinessConstants.MAX_GROUP_SIZE);
            throw new BusinessRuleViolationException(ErrorCode.GROUP_MAX_SIZE_REACHED);
        }

        // Create new user entity for the member
        UserEntity member = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .name(request.memberName())
                .build();
        UserEntity savedMember = userRepository.save(member);

        // Create user-group relationship
        GroupEntity group = userGroupEntityList.get(0).getGroup();
        UserGroupEntity userGroupEntity = UserGroupEntity.builder()
                .userUuid(savedMember.getUuid())
                .groupUuid(groupId)
                .user(savedMember)
                .group(group)
                .build();
        userGroupRepository.save(userGroupEntity);

        logger.info("Member added successfully: groupId={}, memberUuid={}, newMemberCount={}",
                PiiMaskingUtil.maskUuid(groupId),
                PiiMaskingUtil.maskUuid(savedMember.getUuid()),
                currentMemberCount + 1);

        // Build response directly (like joinGroupInvited does)
        List<UserEntity> users = userGroupEntityList.stream()
                .map(UserGroupEntity::getUser)
                .collect(Collectors.toList());
        users.add(savedMember);
        Long transactionCount = transactionRepository.countByGroup_Uuid(groupId);

        return GroupResponseDTO.from(users, group, transactionCount);
    }

    private void validateMaxGroupCount(String uid) {
        List<UserEntity> userEntityList = userRepository.findByAuthUserUid(uid);

        // Check if user has unlimited group privilege
        String unlimitedUid = businessRuleConfig.getUnlimitedGroupUid();
        boolean isUnlimitedUser = unlimitedUid != null
                && !unlimitedUid.isEmpty()
                && uid.equals(unlimitedUid);

        if (!isUnlimitedUser && userEntityList.size() >= BusinessConstants.MAX_GROUP_PARTICIPANTS) {
            throw new BusinessRuleViolationException(ErrorCode.USER_MAX_GROUP_COUNT_EXCEEDED);
        }
    }
}
