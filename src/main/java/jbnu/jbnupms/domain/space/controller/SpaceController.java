package jbnu.jbnupms.domain.space.controller;

import jakarta.validation.Valid;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.space.dto.SpaceCreateRequest;
import jbnu.jbnupms.domain.space.dto.SpaceDetailResponse;
import jbnu.jbnupms.domain.space.dto.SpaceInviteRequest;
import jbnu.jbnupms.domain.space.dto.SpaceResponse;
import jbnu.jbnupms.domain.space.dto.SpaceRoleUpdateRequest;
import jbnu.jbnupms.domain.space.dto.SpaceUpdateRequest;
import jbnu.jbnupms.domain.space.service.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    // 스페이스 생성
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> createSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SpaceCreateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long spaceId = spaceService.createSpace(userId, request);
        return ResponseEntity.ok(CommonResponse.success(spaceId));
    }

    // 스페이스 조회
    @GetMapping("/{spaceId}")
    public ResponseEntity<CommonResponse<SpaceDetailResponse>> getSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long spaceId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(spaceService.getSpace(userId, spaceId)));
    }

    // 스페이스 목록 조회
    @GetMapping
    public ResponseEntity<CommonResponse<List<SpaceResponse>>> getSpaces(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(spaceService.getSpaces(userId)));
    }

    // 스페이스 수정
    @PutMapping("/{spaceId}")
    public ResponseEntity<CommonResponse<Void>> updateSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long spaceId,
            @RequestBody SpaceUpdateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        spaceService.updateSpace(userId, spaceId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // 스페이스 삭제
    @DeleteMapping("/{spaceId}")
    public ResponseEntity<CommonResponse<Void>> deleteSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long spaceId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        spaceService.deleteSpace(userId, spaceId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // 스페이스 멤버 초대
    @PostMapping("/{spaceId}/members")
    public ResponseEntity<CommonResponse<Void>> inviteMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long spaceId,
            @Valid @RequestBody SpaceInviteRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        spaceService.inviteMember(userId, spaceId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // 스페이스 멤버 권한 수정
    @PatchMapping("/{spaceId}/members/{targetUserId}")
    public ResponseEntity<CommonResponse<Void>> updateMemberRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long spaceId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody SpaceRoleUpdateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        spaceService.updateMemberRole(userId, spaceId, targetUserId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // 스페이스 탈퇴 (본인)
    @DeleteMapping("/{spaceId}/leave")
    public ResponseEntity<CommonResponse<Void>> leaveSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long spaceId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        spaceService.leaveSpace(userId, spaceId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // 스페이스 멤버 추방 (관리자)
    @DeleteMapping("/{spaceId}/members/{targetUserId}")
    public ResponseEntity<CommonResponse<Void>> expelMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long spaceId,
            @PathVariable Long targetUserId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        spaceService.expelMember(userId, spaceId, targetUserId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}