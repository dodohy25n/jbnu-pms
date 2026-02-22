package jbnu.jbnupms.domain.file.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.file.dto.FileResponse;
import jbnu.jbnupms.domain.file.entity.ProjectFile;
import jbnu.jbnupms.domain.file.repository.ProjectFileRepository;
import jbnu.jbnupms.domain.file.repository.TaskFileRepository;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectFileService {

    private final ProjectFileRepository projectFileRepository;
    private final TaskFileRepository taskFileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final S3FileService s3FileService;

    /**
     * 프로젝트 파일 업로드
     */
    @Transactional
    public FileResponse uploadProjectFile(Long projectId, MultipartFile file, Long userId) {
        // 프로젝트 존재 확인
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        // 사용자 존재 확인
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 프로젝트 접근 권한 확인 + VIEWER는 업로드 불가
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_ACCESS_DENIED));
        if (member.getRole() == ProjectRole.VIEWER) {
            throw new CustomException(ErrorCode.VIEWER_WRITE_ACCESS_DENIED);
        }

        // S3에 파일 업로드
        String fileUrl = s3FileService.uploadFile(file, "project/" + projectId);

        // DB에 파일 정보 저장
        ProjectFile projectFile = ProjectFile.builder()
                .project(project)
                .uploader(uploader)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .build();

        ProjectFile savedFile = projectFileRepository.save(projectFile);
        log.info("프로젝트 파일 업로드 완료: fileId={}, projectId={}, userId={}",
                savedFile.getId(), projectId, userId);

        return FileResponse.fromProjectFile(savedFile);
    }

    /**
     * 프로젝트 직접 업로드 파일만 조회 (태스크 파일 제외)
     */
    public List<FileResponse> getProjectFiles(Long projectId, Long userId) {
        // 프로젝트 접근 권한 확인 (VIEWER 포함 조회 가능)
        validateProjectAccess(projectId, userId);

        List<ProjectFile> projectFiles = projectFileRepository.findByProjectId(projectId);

        // taskFileId가 NULL인 것만 = 프로젝트 직접 업로드 파일만
        return projectFiles.stream()
                .filter(file -> file.getTaskFileId() == null)
                .map(FileResponse::fromProjectFile)
                .collect(Collectors.toList());
    }

    /**
     * 프로젝트 전체 파일 조회 (프로젝트 직접 파일 + 태스크 파일 모두)
     */
    public List<FileResponse> getAllProjectFiles(Long projectId, Long userId) {
        // 프로젝트 접근 권한 확인 (VIEWER 포함 조회 가능)
        validateProjectAccess(projectId, userId);

        // project_files 테이블에 프로젝트 직접 파일 + 태스크 파일이 모두 있음
        List<ProjectFile> allFiles = projectFileRepository.findByProjectId(projectId);

        return allFiles.stream()
                .map(FileResponse::fromProjectFile)
                .collect(Collectors.toList());
    }

    /**
     * 프로젝트 파일 삭제
     * 태스크 파일에서 생성된 파일은 삭제 불가 (태스크에서만 삭제 가능)
     */
    @Transactional
    public void deleteProjectFile(Long projectId, Long fileId, Long userId) {
        // 파일 존재 확인
        ProjectFile projectFile = projectFileRepository.findByIdWithProjectAndUploader(fileId)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        // 프로젝트 ID 일치 확인
        if (!projectFile.getProject().getId().equals(projectId)) {
            throw new CustomException(ErrorCode.FILE_PROJECT_MISMATCH);
        }

        // 태스크 파일에서 생성된 파일은 삭제 불가
        if (projectFile.isFromTaskFile()) {
            throw new CustomException(ErrorCode.CANNOT_DELETE_TASK_FILE_FROM_PROJECT);
        }

        // 업로더 본인 확인
        if (!projectFile.isUploader(userId)) {
            throw new CustomException(ErrorCode.FILE_DELETE_UNAUTHORIZED);
        }

        // S3에서 파일 삭제
        s3FileService.deleteFile(projectFile.getFileUrl());

        // DB에서 soft delete
        projectFile.softDelete();
        projectFileRepository.save(projectFile);

        log.info("프로젝트 파일 삭제 완료: fileId={}, projectId={}, userId={}",
                fileId, projectId, userId);
    }

    /**
     * 프로젝트 접근 권한 확인
     * 모든 멤버(ADMIN, MEMBER, VIEWER) 조회 가능
     */
    private void validateProjectAccess(Long projectId, Long userId) {
        boolean isProjectMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
        if (!isProjectMember) {
            throw new CustomException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
    }
}