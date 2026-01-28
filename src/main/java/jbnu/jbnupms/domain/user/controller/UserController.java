package jbnu.jbnupms.domain.user.controller;

import jakarta.validation.Valid;
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
    public ResponseEntity<UserResponse> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(userService.getMyInfo(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        Long requestUserId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(userService.updateUser(requestUserId, userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId
    ) {
        Long requestUserId = Long.parseLong(userDetails.getUsername());
        userService.deleteUser(requestUserId, userId);
        return ResponseEntity.ok().build();
    }
}