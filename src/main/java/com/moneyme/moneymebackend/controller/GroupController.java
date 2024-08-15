package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.annotation.UId;
import com.moneyme.moneymebackend.dto.request.CreateGroupRequest;
import com.moneyme.moneymebackend.dto.request.JoinGroupRequest;
import com.moneyme.moneymebackend.dto.response.GetGroupListResponse;
import com.moneyme.moneymebackend.dto.response.GroupDetailsResponse;
import com.moneyme.moneymebackend.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.guieffect.qual.UI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_GROUPS;

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
            @UId String uid,
            @PathVariable("groupId") UUID groupId
    ) {
        GroupDetailsResponse response = service.getGroupInfo(uid, groupId);
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
