package jbnu.jbnupms.domain.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.file.dto.FileResponse;
import jbnu.jbnupms.domain.file.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/projects/{projectId}/files")
@RequiredArgsConstructor
public class ProjectFileController {

    private final ProjectFileService projectFileService;

    @Operation(summary = "프로젝트 파일 업로드")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<FileResponse>> uploadProjectFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {
        Long userId = Long.parseLong(userDetails.getUsername());
        FileResponse response = projectFileService.uploadProjectFile(projectId, file, userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "프로젝트 파일 목록 조회")
    @GetMapping
    public ResponseEntity<CommonResponse<List<FileResponse>>> getProjectFiles(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<FileResponse> responses = projectFileService.getProjectFiles(projectId, userId);
        return ResponseEntity.ok(CommonResponse.success(responses));
    }

    @Operation(summary = "프로젝트 전체 파일 조회 (프로젝트 파일 + 태스크 파일)")
    @GetMapping("/all")
    public ResponseEntity<CommonResponse<List<FileResponse>>> getAllProjectFiles(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<FileResponse> responses = projectFileService.getAllProjectFiles(projectId, userId);
        return ResponseEntity.ok(CommonResponse.success(responses));
    }

    @Operation(summary = "프로젝트 파일 삭제")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<CommonResponse<Void>> deleteProjectFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long fileId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        projectFileService.deleteProjectFile(projectId, fileId, userId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "프로젝트 파일 다운로드")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadProjectFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @PathVariable Long fileId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Resource resource = projectFileService.downloadProjectFile(projectId, fileId,
                userId);

        // s3FileService.downloadFile 등에서 실제 원본 파일명을 헤더에 넣어주려면 여기서 가공하거나,
        // 간단히 다운로드용 헤더를 추가합니다. (DB에서 파일명을 다시 조회할 수도 있지만 여기서는 생략)
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileId + "\"")
                .body(resource);
    }
}