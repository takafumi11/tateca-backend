package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.dto.request.CreateGroupRequest;
import com.moneyme.moneymebackend.dto.request.JoinGroupRequest;
import com.moneyme.moneymebackend.dto.response.GetGroupListResponse;
import com.moneyme.moneymebackend.dto.response.GroupDetailsResponse;
import com.moneyme.moneymebackend.service.GroupService;
import lombok.RequiredArgsConstructor;
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
            @RequestBody CreateGroupRequest request
    ) {
        GroupDetailsResponse response = service.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponse> getGroupInfo(
            @PathVariable("groupId") UUID groupId
    ) {
        GroupDetailsResponse response = service.getGroupInfo(groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list/{userId}")
    public ResponseEntity<GetGroupListResponse> getGroupList(
            @PathVariable("userId") UUID userId
    ) {
        GetGroupListResponse response = service.getGroupList(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponse> joinGroup(
            @RequestBody JoinGroupRequest request,
            @PathVariable("groupId") UUID groupId,
            @RequestHeader("token") String token
    ) {
        GroupDetailsResponse response = service.joinGroup(request, groupId, token);
        return ResponseEntity.ok(response);
    }

}
