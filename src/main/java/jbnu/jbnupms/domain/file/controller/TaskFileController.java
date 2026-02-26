package jbnu.jbnupms.domain.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.file.dto.FileResponse;
import jbnu.jbnupms.domain.file.service.TaskFileService;
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

@Tag(name = "Task File", description = "태스크 파일 API")
@RestController
@RequestMapping("/tasks/{taskId}/files")
@RequiredArgsConstructor
public class TaskFileController {

    private final TaskFileService taskFileService;

    @Operation(summary = "태스크 파일 업로드")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<FileResponse>> uploadTaskFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {
        Long userId = Long.parseLong(userDetails.getUsername());
        FileResponse response = taskFileService.uploadTaskFile(taskId, file, userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "태스크 파일 목록 조회")
    @GetMapping
    public ResponseEntity<CommonResponse<List<FileResponse>>> getTaskFiles(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<FileResponse> responses = taskFileService.getTaskFiles(taskId, userId);
        return ResponseEntity.ok(CommonResponse.success(responses));
    }

    @Operation(summary = "태스크 파일 삭제")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<CommonResponse<Void>> deleteTaskFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @PathVariable Long fileId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskFileService.deleteTaskFile(taskId, fileId, userId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "태스크 파일 다운로드")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadTaskFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @PathVariable Long fileId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Resource resource = taskFileService.downloadTaskFile(taskId, fileId, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileId + "\"")
                .body(resource);
    }
}