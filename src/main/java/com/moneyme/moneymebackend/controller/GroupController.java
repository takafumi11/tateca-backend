package com.moneyme.moneymebackend.controller;

import com.moneyme.moneymebackend.dto.request.CreateGroupRequest;
import com.moneyme.moneymebackend.dto.response.UserGroupsResponse;
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

import static com.moneyme.moneymebackend.constants.ApiConstants.HEADER_AUTHORIZATION;
import static com.moneyme.moneymebackend.constants.ApiConstants.PATH_GROUPS;

@RestController
@RequestMapping(PATH_GROUPS)
@RequiredArgsConstructor
public class GroupController {
    private final GroupService service;

    @GetMapping("/{groupId}")
    public ResponseEntity<UserGroupsResponse> getGroupInfo(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @PathVariable("groupId") UUID groupId
    ) {
        UserGroupsResponse response = service.getGroupInfo(groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UserGroupsResponse> createGroup(
            @RequestHeader(HEADER_AUTHORIZATION) String token,
            @RequestBody CreateGroupRequest request) {
        UserGroupsResponse response = service.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
