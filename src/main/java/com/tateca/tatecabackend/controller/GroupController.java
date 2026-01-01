package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateGroupNameRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Groups", description = "Group management operations")
public class GroupController {
    private final GroupService service;

    @PostMapping
    @Operation(summary = "Create a new group")
    public ResponseEntity<GroupResponseDTO> createGroup(
            @UId String uid,
            @RequestBody CreateGroupRequestDTO request
    ) {
        GroupResponseDTO response = service.createGroup(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{groupId}")
    @Operation(summary = "Update group name")
    public ResponseEntity<GroupResponseDTO> updateGroupName(
            @PathVariable("groupId") UUID groupId,
            @RequestBody UpdateGroupNameRequestDTO request
    ) {
        GroupResponseDTO response = service.updateGroupName(groupId, request.name());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group information by ID")
    public ResponseEntity<GroupResponseDTO> getGroupInfo(
            @PathVariable("groupId") UUID groupId
    ) {
        GroupResponseDTO response = service.getGroupInfo(groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    @Operation(summary = "Get list of groups for the current user")
    public ResponseEntity<GroupListResponseDTO> getGroupList(
            @UId String uid
    ) {
        GroupListResponseDTO response = service.getGroupList(uid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}")
    @Operation(summary = "Join a group using invitation token")
    public ResponseEntity<GroupResponseDTO> joinGroupInvited(
            @RequestBody JoinGroupRequestDTO request,
            @PathVariable("groupId") UUID groupId,
            @UId String uid
    ) {
        GroupResponseDTO response = service.joinGroupInvited(request, groupId, uid);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}/users/{userUuid}")
    @Operation(summary = "Leave a group")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("userUuid") UUID userUuid,
            @UId String uid
    ) {
        service.leaveGroup(groupId, userUuid);
        return ResponseEntity.noContent().build();
    }

}
