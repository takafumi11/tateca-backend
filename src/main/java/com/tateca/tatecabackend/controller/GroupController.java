package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateGroupNameRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.service.GroupService;
import com.tateca.tatecabackend.util.PiiMaskingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

@RestController
@RequestMapping(value = "/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Group management operations")
public class GroupController {
    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);
    private final GroupService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new group")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Group created successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 400,
                          "message": "Invalid request parameters"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<GroupResponseDTO> createGroup(
            @UId String uid,
            @Valid @RequestBody CreateGroupRequestDTO request
    ) {
        logger.info("Creating group: uid={}, groupName={}",
                PiiMaskingUtil.maskUid(uid),
                request.groupName());

        GroupResponseDTO response = service.createGroup(uid, request);

        if (response != null && response.groupInfo() != null && response.groupInfo().uuid() != null) {
            logger.info("Group created successfully: groupId={}, uid={}",
                    PiiMaskingUtil.maskUuid(UUID.fromString(response.groupInfo().uuid())),
                    PiiMaskingUtil.maskUid(uid));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping(value = "/{groupId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update group name")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Group name updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 400,
                          "message": "Invalid request parameters"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Group not found"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User doesn't have permission to update this group",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 403,
                          "message": "Forbidden"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<GroupResponseDTO> updateGroupName(
            @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody UpdateGroupNameRequestDTO request
    ) {
        GroupResponseDTO response = service.updateGroupName(groupId, request.groupName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group information by ID")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Group information retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Group not found"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<GroupResponseDTO> getGroupInfo(
            @PathVariable("groupId") UUID groupId
    ) {
        GroupResponseDTO response = service.getGroupInfo(groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    @Operation(summary = "Get list of groups for the current user")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Group list retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<GroupListResponseDTO> getGroupList(
            @UId String uid
    ) {
        GroupListResponseDTO response = service.getGroupList(uid);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{groupId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Join a group using invitation token")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User joined group successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid invitation token or request parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 400,
                          "message": "Invalid invitation token"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Group not found"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - User already member of the group",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 409,
                          "message": "User already member of the group"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<GroupResponseDTO> joinGroupInvited(
            @Valid @RequestBody JoinGroupRequestDTO request,
            @PathVariable("groupId") UUID groupId,
            @UId String uid
    ) {
        logger.info("User joining group: groupId={}, uid={}",
                PiiMaskingUtil.maskUuid(groupId),
                PiiMaskingUtil.maskUid(uid));

        GroupResponseDTO response = service.joinGroupInvited(request, groupId, uid);

        if (response != null && response.users() != null) {
            logger.info("User joined group successfully: groupId={}, uid={}, memberCount={}",
                    PiiMaskingUtil.maskUuid(groupId),
                    PiiMaskingUtil.maskUid(uid),
                    response.users().size());
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}/users/{userUuid}")
    @Operation(summary = "Leave a group")
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "User left group successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Group or user not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Group or user not found"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 401,
                          "message": "Unauthorized"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User doesn't have permission to remove this user",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 403,
                          "message": "Forbidden"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Void> leaveGroup(
            @PathVariable("groupId") UUID groupId,
            @PathVariable("userUuid") UUID userUuid
    ) {
        logger.info("User leaving group: groupId={}, userUuid={}",
                PiiMaskingUtil.maskUuid(groupId),
                PiiMaskingUtil.maskUuid(userUuid));

        service.leaveGroup(groupId, userUuid);

        logger.info("User left group successfully: groupId={}, userUuid={}",
                PiiMaskingUtil.maskUuid(groupId),
                PiiMaskingUtil.maskUuid(userUuid));

        return ResponseEntity.noContent().build();
    }

}
