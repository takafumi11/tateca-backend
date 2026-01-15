package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.AddMemberRequestDTO;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;

import java.util.UUID;

public interface GroupService {
    GroupResponseDTO getGroupInfo(UUID groupId);

    GroupResponseDTO updateGroupName(UUID groupId, String name);

    GroupListResponseDTO getGroupList(String uid);

    GroupResponseDTO createGroup(String uid, CreateGroupRequestDTO request);

    GroupResponseDTO joinGroupInvited(JoinGroupRequestDTO request, UUID groupId, String uid);

    void leaveGroup(UUID groupId, UUID userUuid);

    GroupResponseDTO addMember(UUID groupId, String uid, AddMemberRequestDTO request);
}
