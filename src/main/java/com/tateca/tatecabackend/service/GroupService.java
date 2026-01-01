package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupDetailsResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;

import java.util.UUID;

public interface GroupService {
    GroupDetailsResponseDTO getGroupInfo(UUID groupId);

    GroupDetailsResponseDTO updateGroupName(UUID groupId, String name);

    GroupListResponseDTO getGroupList(String uid);

    GroupDetailsResponseDTO createGroup(String uid, CreateGroupRequestDTO request);

    GroupDetailsResponseDTO joinGroupInvited(JoinGroupRequestDTO request, UUID groupId, String uid);

    void leaveGroup(UUID groupId, UUID userUuid);
}
