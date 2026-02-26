package jbnu.jbnupms.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.user.dto.DeleteUserRequest;
import jbnu.jbnupms.domain.user.dto.UpdateUserRequest;
import jbnu.jbnupms.domain.user.dto.UserResponse;
import jbnu.jbnupms.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserResponse>> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(userService.getMyInfo(userId)));
    }

    @Operation(summary = "사용자 정보 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(CommonResponse.success(userService.getUserById(userId)));
    }

    @Operation(summary = "사용자 정보 수정")
    @PatchMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserResponse>> updateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        Long requestUserId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(userService.updateUser(requestUserId, userId, request)));
    }

    @Operation(summary = "프로필 이미지 수정")
    @PatchMapping(value = "/{userId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<UserResponse>> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId,
            @RequestParam("image") MultipartFile image
    ) {
        Long requestUserId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(userService.updateProfileImage(requestUserId, userId, image)));
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/{userId}")
    public ResponseEntity<CommonResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId,
            @Valid @RequestBody DeleteUserRequest request) {
        Long requestUserId = Long.parseLong(userDetails.getUsername());
        userService.deleteUser(requestUserId, userId, request.getReason());
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}