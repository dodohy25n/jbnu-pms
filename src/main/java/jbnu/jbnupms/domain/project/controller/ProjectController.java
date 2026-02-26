package jbnu.jbnupms.domain.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.project.dto.ProjectCreateRequest;
import jbnu.jbnupms.domain.project.dto.ProjectInviteRequest;
import jbnu.jbnupms.domain.project.dto.ProjectResponse;
import jbnu.jbnupms.domain.project.dto.ProjectRoleUpdateRequest;
import jbnu.jbnupms.domain.project.dto.ProjectUpdateRequest;
import jbnu.jbnupms.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Project", description = "프로젝트 API")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 생성")
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProjectCreateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long projectId = projectService.createProject(userId, request);
        return ResponseEntity.ok(CommonResponse.success(projectId));
    }

    @Operation(summary = "특정 스페이스 프로젝트 목록 조회")
    @GetMapping
    public ResponseEntity<CommonResponse<List<ProjectResponse>>> getProjects(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long spaceId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(projectService.getProjects(userId, spaceId)));
    }

    @Operation(summary = "프로젝트 상세 조회")
    @GetMapping("/{projectId}")
    public ResponseEntity<CommonResponse<ProjectResponse>> getProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(projectService.getProject(userId, projectId)));
    }

    @Operation(summary = "프로젝트 수정")
    @PatchMapping("/{projectId}")
    public ResponseEntity<CommonResponse<Void>> updateProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        projectService.updateProject(userId, projectId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "프로젝트 삭제")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<CommonResponse<Void>> deleteProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        projectService.deleteProject(userId, projectId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "프로젝트 멤버 초대")
    @PostMapping("/{projectId}/members")
    public ResponseEntity<CommonResponse<Void>> inviteMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectInviteRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        projectService.inviteMember(userId, projectId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "프로젝트 멤버 권한 수정")
    @PatchMapping("/{projectId}/members/{targetUserId}")
    public ResponseEntity<CommonResponse<Void>> updateMemberRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody ProjectRoleUpdateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        projectService.updateMemberRole(userId, projectId, targetUserId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "프로젝트 멤버 탈퇴")
    @DeleteMapping("/{projectId}/members/{targetUserId}")
    public ResponseEntity<CommonResponse<Void>> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long targetUserId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        projectService.removeMember(userId, projectId, targetUserId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}