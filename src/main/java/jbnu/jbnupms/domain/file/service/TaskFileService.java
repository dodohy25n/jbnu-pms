package jbnu.jbnupms.domain.file.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.file.dto.FileResponse;
import jbnu.jbnupms.domain.file.entity.ProjectFile;
import jbnu.jbnupms.domain.file.entity.TaskFile;
import jbnu.jbnupms.domain.file.repository.ProjectFileRepository;
import jbnu.jbnupms.domain.file.repository.TaskFileRepository;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.repository.TaskRepository;
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
public class TaskFileService {

    private final TaskFileRepository taskFileRepository;
    private final ProjectFileRepository projectFileRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final S3FileService s3FileService;

    /**
     * 태스크 파일 업로드
     * 태스크에 업로드되는 파일은 자동으로 해당 프로젝트에도 포함됨
     */
    @Transactional
    public FileResponse uploadTaskFile(Long taskId, MultipartFile file, Long userId) {
        // 태스크 존재 확인
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        Project project = task.getProject();

        // 사용자 존재 확인
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 태스크 접근 권한 확인 (프로젝트 멤버인지)
        validateTaskAccess(task, userId);

        // VIEWER는 파일 업로드 불가
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_ACCESS_DENIED));
        if (member.getRole() == ProjectRole.VIEWER) {
            throw new CustomException(ErrorCode.VIEWER_WRITE_ACCESS_DENIED);
        }

        // S3에 파일 업로드
        String fileUrl = s3FileService.uploadFile(file, "project/" + project.getId() + "/task/" + taskId);

        // 1. task_files 테이블에 저장
        TaskFile taskFile = TaskFile.builder()
                .task(task)
                .project(project)
                .uploader(uploader)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .build();

        TaskFile savedTaskFile = taskFileRepository.save(taskFile);

        // 2. project_files 테이블에도 동시 저장 (taskFileId 포함)
        ProjectFile projectFile = ProjectFile.builder()
                .project(project)
                .uploader(uploader)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .taskFileId(savedTaskFile.getId())
                .build();

        projectFileRepository.save(projectFile);

        log.info("태스크 파일 업로드 완료 (task_files + project_files 동시 저장): taskFileId={}, taskId={}, projectId={}, userId={}",
                savedTaskFile.getId(), taskId, project.getId(), userId);

        return FileResponse.fromTaskFile(savedTaskFile);
    }

    /**
     * 태스크 파일 목록 조회
     */
    public List<FileResponse> getTaskFiles(Long taskId, Long userId) {
        // 태스크 존재 확인
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        // 태스크 접근 권한 확인 (VIEWER 포함 조회 가능)
        validateTaskAccess(task, userId);

        List<TaskFile> taskFiles = taskFileRepository.findByTaskId(taskId);
        return taskFiles.stream()
                .map(FileResponse::fromTaskFile)
                .collect(Collectors.toList());
    }

    /**
     * 태스크 파일 삭제
     * task_files와 project_files에서 모두 삭제
     */
    @Transactional
    public void deleteTaskFile(Long taskId, Long fileId, Long userId) {
        // 파일 존재 확인
        TaskFile taskFile = taskFileRepository.findByIdWithTaskAndProjectAndUploader(fileId)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        // 태스크 ID 일치 확인
        if (!taskFile.getTask().getId().equals(taskId)) {
            throw new CustomException(ErrorCode.FILE_TASK_MISMATCH);
        }

        // 업로더 본인 확인
        if (!taskFile.isUploader(userId)) {
            throw new CustomException(ErrorCode.FILE_DELETE_UNAUTHORIZED);
        }

        // S3에서 파일 삭제
        s3FileService.deleteFile(taskFile.getFileUrl());

        // 1. task_files에서 soft delete
        taskFile.softDelete();
        taskFileRepository.save(taskFile);

        // 2. project_files에서 taskFileId로 빠르게 찾아서 soft delete
        projectFileRepository.findByTaskFileId(fileId).ifPresent(projectFile -> {
            projectFile.softDelete();
            projectFileRepository.save(projectFile);
        });

        log.info("태스크 파일 삭제 완료 (task_files + project_files): fileId={}, taskId={}, userId={}",
                fileId, taskId, userId);
    }

    /**
     * Task 접근 권한 확인
     * 프로젝트 멤버인지만 확인 (담당자 여부와 무관하게 동일 권한)
     */
    private void validateTaskAccess(Task task, Long userId) {
        boolean isProjectMember = projectMemberRepository.existsByProjectIdAndUserId(
                task.getProject().getId(), userId);
        if (!isProjectMember) {
            throw new CustomException(ErrorCode.TASK_ACCESS_DENIED);
        }
    }
}