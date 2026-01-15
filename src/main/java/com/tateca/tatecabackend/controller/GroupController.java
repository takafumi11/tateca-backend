package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.annotation.UId;
import com.tateca.tatecabackend.dto.request.AddMemberRequestDTO;
import com.tateca.tatecabackend.dto.request.CreateGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.JoinGroupRequestDTO;
import com.tateca.tatecabackend.dto.request.UpdateGroupNameRequestDTO;
import com.tateca.tatecabackend.dto.response.GroupListResponseDTO;
import com.tateca.tatecabackend.dto.response.GroupResponseDTO;
import com.tateca.tatecabackend.exception.ErrorResponse;
import com.tateca.tatecabackend.service.GroupService;
import com.tateca.tatecabackend.util.PiiMaskingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Empty or blank group name",
                        description = "When group_name is null, empty string, or only whitespace",
                        value = """
                            {
                              "status": 400,
                              "message": "groupName: Group name is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Group name exceeds maximum length",
                        description = "When group_name exceeds 100 characters",
                        value = """
                            {
                              "status": 400,
                              "message": "groupName: Group name must be between 1 and 100 characters"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Empty or blank your name",
                        description = "When host_name is null, empty string, or only whitespace",
                        value = """
                            {
                              "status": 400,
                              "message": "yourName: Your name is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Your name exceeds maximum length",
                        description = "When host_name exceeds 50 characters",
                        value = """
                            {
                              "status": 400,
                              "message": "yourName: Your name must be between 1 and 50 characters"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Empty member names list",
                        description = "When participants_name list is empty",
                        value = """
                            {
                              "status": 400,
                              "message": "memberNames: Member names must be between 1 and 9"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Member names list exceeds maximum size",
                        description = "When participants_name list has more than 9 members",
                        value = """
                            {
                              "status": 400,
                              "message": "memberNames: Member names must be between 1 and 9"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Member name is blank",
                        description = "When a member name in the list is blank",
                        value = """
                            {
                              "status": 400,
                              "message": "memberNames[]: Member name cannot be blank"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Member name exceeds maximum length",
                        description = "When a member name exceeds 50 characters",
                        value = """
                            {
                              "status": 400,
                              "message": "memberNames[]: Member name must be between 1 and 50 characters"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
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
            responseCode = "409",
            description = "Conflict - User has reached maximum group limit",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 409,
                          "message": "can't join more than 10 groups"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "415",
            description = "Unsupported Media Type - Content-Type header must be application/json",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 415,
                          "message": "Unsupported Media Type. Content-Type must be application/json"
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
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Empty or blank group name",
                        description = "When group_name is null, empty string, or only whitespace",
                        value = """
                            {
                              "status": 400,
                              "message": "groupName: Group name is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Group name exceeds maximum length",
                        description = "When group_name exceeds 100 characters",
                        value = """
                            {
                              "status": 400,
                              "message": "groupName: Group name must be between 1 and 100 characters"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid UUID format",
                        description = "When groupId path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid format for parameter 'groupId': expected UUID"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
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
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 403,
                          "message": "Forbidden"
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
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "group not found"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "415",
            description = "Unsupported Media Type - Content-Type header must be application/json",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 415,
                          "message": "Unsupported Media Type. Content-Type must be application/json"
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
            responseCode = "400",
            description = "Validation error - Invalid UUID format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Invalid UUID format",
                    description = "When groupId path parameter has invalid UUID format",
                    value = """
                        {
                          "status": 400,
                          "message": "Invalid format for parameter 'groupId': expected UUID"
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
                schema = @Schema(implementation = ErrorResponse.class),
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
            responseCode = "404",
            description = "Group not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Group Not Found"
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
                schema = @Schema(implementation = ErrorResponse.class),
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
            description = "Validation error - Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "User UUID is null",
                        description = "When user_uuid is null",
                        value = """
                            {
                              "status": 400,
                              "message": "userUuid: User UUID is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Join token is null",
                        description = "When join_token is null",
                        value = """
                            {
                              "status": 400,
                              "message": "joinToken: Join token is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid UUID format for user_uuid",
                        description = "When user_uuid has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid value for field 'user_uuid': expected UUID but received String"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid UUID format for groupId",
                        description = "When groupId path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid format for parameter 'groupId': expected UUID"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
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
            description = "Forbidden - Invalid join token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 403,
                          "message": "Invalid join token"
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
                schema = @Schema(implementation = ErrorResponse.class),
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
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 409,
                          "message": "already joined this group"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "415",
            description = "Unsupported Media Type - Content-Type header must be application/json",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 415,
                          "message": "Unsupported Media Type. Content-Type must be application/json"
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
            responseCode = "400",
            description = "Validation error - Invalid UUID format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid groupId UUID format",
                        description = "When groupId path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid format for parameter 'groupId': expected UUID"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid userUuid UUID format",
                        description = "When userUuid path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid format for parameter 'userUuid': expected UUID"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
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
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 403,
                          "message": "Forbidden"
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
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 404,
                          "message": "Group not found"
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

    @PostMapping(value = "/{groupId}/members", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new member to an existing group")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Member added successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error - Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Empty or blank member name",
                        description = "When member_name is null, empty string, or only whitespace",
                        value = """
                            {
                              "status": 400,
                              "message": "memberName: Member name is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Member name exceeds maximum length",
                        description = "When member_name exceeds 50 characters",
                        value = """
                            {
                              "status": 400,
                              "message": "memberName: Member name must be between 1 and 50 characters"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid UUID format",
                        description = "When groupId path parameter has invalid UUID format",
                        value = """
                            {
                              "status": 400,
                              "message": "Invalid format for parameter 'groupId': expected UUID"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing Firebase JWT token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
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
            description = "Forbidden - User is not a member of this group",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 403,
                          "message": "Only group members can add members"
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
                schema = @Schema(implementation = ErrorResponse.class),
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
            description = "Conflict - Group has reached maximum size",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 409,
                          "message": "Group has reached maximum size of 10 members"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "415",
            description = "Unsupported Media Type - Content-Type header must be application/json",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "status": 415,
                          "message": "Unsupported Media Type. Content-Type must be application/json"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<GroupResponseDTO> addMember(
            @PathVariable("groupId") UUID groupId,
            @UId String uid,
            @Valid @RequestBody AddMemberRequestDTO request
    ) {
        logger.info("Adding member to group: groupId={}, uid={}, memberName={}",
                PiiMaskingUtil.maskUuid(groupId),
                PiiMaskingUtil.maskUid(uid),
                request.memberName());

        GroupResponseDTO response = service.addMember(groupId, uid, request);

        if (response != null && response.users() != null) {
            logger.info("Member added successfully: groupId={}, uid={}, newMemberCount={}",
                    PiiMaskingUtil.maskUuid(groupId),
                    PiiMaskingUtil.maskUid(uid),
                    response.users().size());
        }

        return ResponseEntity.ok(response);
    }

}
