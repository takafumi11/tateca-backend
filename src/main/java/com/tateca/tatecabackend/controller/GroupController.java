package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateGroupNameRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupDetailsResponseDTO;
import com.tateca.tatecabackend.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tateca.tatecabackend.constants.ApiConstants.PATH_GROUPS;

@RestController
@RequestMapping(PATH_GROUPS)
@RequiredArgsConstructor
public class GroupController {
    private final GroupService service;

    @PostMapping
    public ResponseEntity<GroupDetailsResponseDTO> createGroup(
            @UId String uid,
            @RequestBody CreateGroupRequestDTO request
    ) {
        GroupDetailsResponseDTO response = service.createGroup(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponseDTO> updateGroupName(
            @PathVariable("groupId") UUID groupId,
            @RequestBody UpdateGroupNameRequestDTO request
    ) {
        GroupDetailsResponseDTO response = service.updateGroupName(groupId, request.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponseDTO> getGroupInfo(
            @PathVariable("groupId") UUID groupId
    ) {
        GroupDetailsResponseDTO response = service.getGroupInfo(groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<GroupListResponseDTO> getGroupList(
            @UId String uid
    ) {
        GroupListResponseDTO response = service.getGroupList(uid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponseDTO> joinGroupInvited(
            @RequestBody JoinGroupRequestDTO request,
            @PathVariable("groupId") UUID groupId,
            @UId String uid
    ) {
        GroupDetailsResponseDTO response = service.joinGroupInvited(request, groupId, uid);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}/users/{userUuid}")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("userUuid") UUID userUuid,
            @UId String uid
    ) {
        service.leaveGroup(groupId, userUuid);
        return ResponseEntity.noContent().build();
    }

}
