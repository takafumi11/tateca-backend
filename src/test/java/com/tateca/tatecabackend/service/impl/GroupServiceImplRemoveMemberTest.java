package com.tateca.tatecabackend.service.impl;

import com.tateca.tatecabackend.entity.AuthUserEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.exception.ErrorCode;
import com.tateca.tatecabackend.exception.domain.BusinessRuleViolationException;
import com.tateca.tatecabackend.exception.domain.EntityNotFoundException;
import com.tateca.tatecabackend.exception.domain.ForbiddenException;
import com.tateca.tatecabackend.repository.ObligationRepository;
import com.tateca.tatecabackend.repository.TransactionRepository;
import com.tateca.tatecabackend.repository.UserGroupRepository;
import com.tateca.tatecabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupServiceImpl — removeMember")
class GroupServiceImplRemoveMemberTest {

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ObligationRepository obligationRepository;

    @InjectMocks
    private GroupServiceImpl service;

    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_UUID = UUID.randomUUID();
    private static final UUID REQUESTER_USER_UUID = UUID.randomUUID();
    private static final String REQUESTER_UID = "firebase-uid-requester";
    private static final String OTHER_UID = "firebase-uid-other";

    private GroupEntity group;
    private AuthUserEntity requesterAuth;
    private UserEntity requesterUser;
    private UserEntity unjoinedTargetUser;
    private UserGroupEntity requesterUserGroup;
    private UserGroupEntity targetUserGroup;

    @BeforeEach
    void setUp() {
        group = GroupEntity.builder()
                .uuid(GROUP_ID)
                .name("Test Group")
                .joinToken(UUID.randomUUID())
                .build();

        requesterAuth = AuthUserEntity.builder()
                .uid(REQUESTER_UID)
                .name("Requester")
                .email("requester@example.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        requesterUser = UserEntity.builder()
                .uuid(REQUESTER_USER_UUID)
                .name("Requester")
                .authUser(requesterAuth)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        unjoinedTargetUser = UserEntity.builder()
                .uuid(TARGET_USER_UUID)
                .name("Unjoined Member")
                .authUser(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        requesterUserGroup = UserGroupEntity.builder()
                .userUuid(REQUESTER_USER_UUID)
                .groupUuid(GROUP_ID)
                .user(requesterUser)
                .group(group)
                .build();

        targetUserGroup = UserGroupEntity.builder()
                .userUuid(TARGET_USER_UUID)
                .groupUuid(GROUP_ID)
                .user(unjoinedTargetUser)
                .group(group)
                .build();
    }

    @Nested
    @DisplayName("Given group does not exist")
    class WhenGroupNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException with GROUP.NOT_FOUND")
        void shouldThrowEntityNotFoundException() {
            when(userGroupRepository.findByGroupUuidWithUserDetails(GROUP_ID))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.removeMember(GROUP_ID, TARGET_USER_UUID, REQUESTER_UID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.GROUP_NOT_FOUND.getCode());

            verify(userRepository, never()).delete(any());
            verify(userGroupRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Given requester is not a group member")
    class WhenRequesterNotGroupMember {

        @Test
        @DisplayName("Should throw ForbiddenException with USER.NOT_GROUP_MEMBER")
        void shouldThrowForbiddenException() {
            when(userGroupRepository.findByGroupUuidWithUserDetails(GROUP_ID))
                    .thenReturn(List.of(requesterUserGroup, targetUserGroup));

            assertThatThrownBy(() -> service.removeMember(GROUP_ID, TARGET_USER_UUID, OTHER_UID))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_GROUP_MEMBER.getCode());

            verify(userRepository, never()).delete(any());
            verify(userGroupRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Given target member is not in the group")
    class WhenTargetNotInGroup {

        @Test
        @DisplayName("Should throw EntityNotFoundException with USER.NOT_FOUND")
        void shouldThrowEntityNotFoundException() {
            UUID nonExistentUserUuid = UUID.randomUUID();
            when(userGroupRepository.findByGroupUuidWithUserDetails(GROUP_ID))
                    .thenReturn(List.of(requesterUserGroup, targetUserGroup));

            assertThatThrownBy(() -> service.removeMember(GROUP_ID, nonExistentUserUuid, REQUESTER_UID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());

            verify(userRepository, never()).delete(any());
            verify(userGroupRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Given target member is already joined (authenticated)")
    class WhenTargetIsJoined {

        @Test
        @DisplayName("Should throw BusinessRuleViolationException with MEMBER.ALREADY_JOINED")
        void shouldThrowBusinessRuleViolationException() {
            AuthUserEntity targetAuth = AuthUserEntity.builder()
                    .uid("firebase-uid-target")
                    .name("Joined Member")
                    .email("joined@example.com")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UserEntity joinedTargetUser = UserEntity.builder()
                    .uuid(TARGET_USER_UUID)
                    .name("Joined Member")
                    .authUser(targetAuth)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UserGroupEntity joinedTargetUserGroup = UserGroupEntity.builder()
                    .userUuid(TARGET_USER_UUID)
                    .groupUuid(GROUP_ID)
                    .user(joinedTargetUser)
                    .group(group)
                    .build();

            when(userGroupRepository.findByGroupUuidWithUserDetails(GROUP_ID))
                    .thenReturn(List.of(requesterUserGroup, joinedTargetUserGroup));

            assertThatThrownBy(() -> service.removeMember(GROUP_ID, TARGET_USER_UUID, REQUESTER_UID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_ALREADY_JOINED.getCode());

            verify(userRepository, never()).delete(any());
            verify(userGroupRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Given requester tries to remove themselves")
    class WhenRequesterRemovesSelf {

        @Test
        @DisplayName("Should throw BusinessRuleViolationException with MEMBER.ALREADY_JOINED (self is always joined)")
        void shouldThrowBusinessRuleViolationException() {
            when(userGroupRepository.findByGroupUuidWithUserDetails(GROUP_ID))
                    .thenReturn(List.of(requesterUserGroup, targetUserGroup));

            assertThatThrownBy(() -> service.removeMember(GROUP_ID, REQUESTER_USER_UUID, REQUESTER_UID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_ALREADY_JOINED.getCode());

            verify(userRepository, never()).delete(any());
            verify(userGroupRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Given target is unjoined but involved as a payer in transactions")
    class WhenTargetIsPayer {

        @Test
        @DisplayName("Should throw BusinessRuleViolationException with MEMBER.HAS_TRANSACTIONS")
        void shouldThrowBusinessRuleViolationException() {
            when(userGroupRepository.findByGroupUuidWithUserDetails(GROUP_ID))
                    .thenReturn(List.of(requesterUserGroup, targetUserGroup));
            when(transactionRepository.existsByPayer(unjoinedTargetUser))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.removeMember(GROUP_ID, TARGET_USER_UUID, REQUESTER_UID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_HAS_TRANSACTIONS.getCode());

            verify(userRepository, never()).delete(any());
            verify(userGroupRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Given target is unjoined but involved as an obligor in transactions")
    class WhenTargetIsObligor {

        @Test
        @DisplayName("Should throw BusinessRuleViolationException with MEMBER.HAS_TRANSACTIONS")
        void shouldThrowBusinessRuleViolationException() {
            when(userGroupRepository.findByGroupUuidWithUserDetails(GROUP_ID))
                    .thenReturn(List.of(requesterUserGroup, targetUserGroup));
            when(transactionRepository.existsByPayer(unjoinedTargetUser))
                    .thenReturn(false);
            when(obligationRepository.existsByUser(unjoinedTargetUser))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.removeMember(GROUP_ID, TARGET_USER_UUID, REQUESTER_UID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_HAS_TRANSACTIONS.getCode());

            verify(userRepository, never()).delete(any());
            verify(userGroupRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Given target is an unjoined member with no transaction involvement")
    class WhenSuccessfulRemoval {

        @Test
        @DisplayName("Should delete UserGroupEntity then UserEntity")
        void shouldDeleteInCorrectOrder() {
            when(userGroupRepository.findByGroupUuidWithUserDetails(GROUP_ID))
                    .thenReturn(List.of(requesterUserGroup, targetUserGroup));
            when(transactionRepository.existsByPayer(unjoinedTargetUser))
                    .thenReturn(false);
            when(obligationRepository.existsByUser(unjoinedTargetUser))
                    .thenReturn(false);

            service.removeMember(GROUP_ID, TARGET_USER_UUID, REQUESTER_UID);

            InOrder inOrder = inOrder(userGroupRepository, userRepository);
            inOrder.verify(userGroupRepository).delete(targetUserGroup);
            inOrder.verify(userRepository).delete(unjoinedTargetUser);
        }

        @Test
        @DisplayName("Should not delete requester's records")
        void shouldNotDeleteRequester() {
            when(userGroupRepository.findByGroupUuidWithUserDetails(GROUP_ID))
                    .thenReturn(List.of(requesterUserGroup, targetUserGroup));
            when(transactionRepository.existsByPayer(unjoinedTargetUser))
                    .thenReturn(false);
            when(obligationRepository.existsByUser(unjoinedTargetUser))
                    .thenReturn(false);

            service.removeMember(GROUP_ID, TARGET_USER_UUID, REQUESTER_UID);

            verify(userGroupRepository).delete(targetUserGroup);
            verify(userRepository).delete(unjoinedTargetUser);
            verify(userGroupRepository, never()).delete(requesterUserGroup);
            verify(userRepository, never()).delete(requesterUser);
        }
    }
}
