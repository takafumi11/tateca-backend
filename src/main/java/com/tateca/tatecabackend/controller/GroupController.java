package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.request.CreateGroupRequest;
import com.tateca.tatecabackend.dto.request.JoinGroupRequest;
import com.tateca.tatecabackend.dto.response.GetGroupListResponse;
import com.tateca.tatecabackend.dto.response.GroupDetailsResponse;
import com.tateca.tatecabackend.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<GroupDetailsResponse> createGroup(
            @UId String uid,
            @RequestBody CreateGroupRequest request
    ) {
        GroupDetailsResponse response = service.createGroup(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponse> getGroupInfo(
            @PathVariable("groupId") UUID groupId
    ) {
        GroupDetailsResponse response = service.getGroupInfo(groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<GetGroupListResponse> getGroupList(
            @UId String uid
    ) {
        GetGroupListResponse response = service.getGroupList(uid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponse> joinGroupInvited(
            @RequestBody JoinGroupRequest request,
            @PathVariable("groupId") UUID groupId,
            @UId String uid
    ) {
        GroupDetailsResponse response = service.joinGroupInvited(request, groupId, uid);
        return ResponseEntity.ok(response);
    }

}
