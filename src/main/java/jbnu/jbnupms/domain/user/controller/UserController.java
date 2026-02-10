package jbnu.jbnupms.domain.user.controller;

import jakarta.validation.Valid;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.user.dto.DeleteUserRequest;
import jbnu.jbnupms.domain.user.dto.UpdateUserRequest;
import jbnu.jbnupms.domain.user.dto.UserResponse;
import jbnu.jbnupms.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserResponse>> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(userService.getMyInfo(userId)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(CommonResponse.success(userService.getUserById(userId)));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserResponse>> updateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        Long requestUserId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(userService.updateUser(requestUserId, userId, request)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<CommonResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId,
            @Valid @RequestBody DeleteUserRequest request
    ) {
        Long requestUserId = Long.parseLong(userDetails.getUsername());
        userService.deleteUser(requestUserId, userId, request.getReason());
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}
