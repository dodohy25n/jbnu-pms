package jbnu.jbnupms.domain.task.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.task.dto.TaskCreateRequest;
import jbnu.jbnupms.domain.task.dto.TaskResponse;
import jbnu.jbnupms.domain.task.dto.TaskUpdateRequest;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskAssignee;
import jbnu.jbnupms.domain.task.repository.TaskAssigneeRepository;
import jbnu.jbnupms.domain.task.repository.TaskRepository;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    // 태스크 생성
    @Transactional
    public Long createTask(Long userId, TaskCreateRequest request) {
        User user = this.getUser(userId);
        Project project = this.getProject(request.getProjectId());
        
        // 프로젝트 멤버인지 확인
        this.validateProjectMember(project.getId(), user.getId());

        Task parent = null;
        if (request.getParentId() != null) {
            parent = taskRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "상위 태스크를 찾을 수 없습니다."));
            
            // 상위 태스크가 같은 프로젝트인지 확인
            if (!parent.getProject().getId().equals(project.getId())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "상위 태스크가 다른 프로젝트에 속해 있습니다.");
            }
        }

        Task task = Task.builder()
                .project(project)
                .creator(user)
                .parent(parent)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .build();

        taskRepository.save(task);

        // 담당자 할당
        if (request.getAssigneeIds() != null && !request.getAssigneeIds().isEmpty()) {
            for (Long assigneeId : request.getAssigneeIds()) {
                assignUserToTask(task, assigneeId);
            }
        }

        return task.getId();
    }

    // 프로젝트별 태스크 목록 조회 (계층형)
    public List<TaskResponse> getTasks(Long userId, Long projectId) {

        // 프로젝트 접근 권한 확인
        this.validateProjectMember(projectId, userId);

        List<Task> rootTasks = taskRepository.findRootTasksByProjectId(projectId);
        
        // 프로젝트 내 모든 담당자 조회 후 Map으로 그룹화
        List<TaskAssignee> allAssignees = taskAssigneeRepository.findAllByTask_ProjectId(projectId);
        Map<Long, List<TaskAssignee>> assigneeMap = allAssignees.stream()
                .collect(Collectors.groupingBy(ta -> ta.getTask().getId()));
        
        // Root Task 목록 스트림으로 순회하며 Map 전달
        return rootTasks.stream()
            .map(task -> TaskResponse.from(task, assigneeMap))
            .collect(Collectors.toList());
    }

    // 태스크 단건 조회
    public TaskResponse getTask(Long userId, Long taskId) {
        Task task = this.getTaskById(taskId);
        Long projectId = task.getProject().getId();
        this.validateProjectMember(projectId, userId);
        
        // 프로젝트 내 모든 담당자 조회 후 Map으로 그룹화 (하위 작업 담당자 포함을 위해)
        List<TaskAssignee> allAssignees = taskAssigneeRepository.findAllByTask_ProjectId(projectId);
        Map<Long, List<TaskAssignee>> assigneeMap = allAssignees.stream()
                .collect(Collectors.groupingBy(ta -> ta.getTask().getId()));

        return TaskResponse.from(task, assigneeMap);
    }

    // 태스크 수정
    @Transactional
    public void updateTask(Long userId, Long taskId, TaskUpdateRequest request) {
        Task task = this.getTaskById(taskId);
        this.validateProjectMember(task.getProject().getId(), userId);

        task.update(
                request.getTitle() != null ? request.getTitle() : task.getTitle(),
                request.getDescription() != null ? request.getDescription() : task.getDescription(),
                request.getStatus() != null ? request.getStatus() : task.getStatus(),
                request.getPriority() != null ? request.getPriority() : task.getPriority(),
                request.getDueDate() != null ? request.getDueDate() : task.getDueDate(),
                request.getProgress() != null ? request.getProgress() : task.getProgress()
        );
    }

    // 태스크 삭제
    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = this.getTaskById(taskId);
        this.validateProjectMember(task.getProject().getId(), userId);
        
        taskRepository.delete(task);
    }

    // 담당자 추가
    @Transactional
    public void addAssignee(Long userId, Long taskId, Long assigneeId) {
        Task task = this.getTaskById(taskId);
        this.validateProjectMember(task.getProject().getId(), userId);

        assignUserToTask(task, assigneeId);
    }

    // 담당자 삭제
    @Transactional
    public void removeAssignee(Long userId, Long taskId, Long assigneeId) {
        Task task = this.getTaskById(taskId);
        this.validateProjectMember(task.getProject().getId(), userId);

        User assignee = this.getUser(assigneeId);
        
        taskAssigneeRepository.deleteByTaskAndUser(task, assignee);
    }

    private void assignUserToTask(Task task, Long assigneeId) {
        User assignee = this.getUser(assigneeId);
        // 담당자도 프로젝트 멤버여야 함
        this.validateProjectMember(task.getProject().getId(), assigneeId);

        if (!taskAssigneeRepository.existsByTaskAndUser(task, assignee)) {
            taskAssigneeRepository.save(TaskAssignee.builder()
                    .task(task)
                    .user(assignee)
                    .build());
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "프로젝트를 찾을 수 없습니다."));
    }

    private Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "태스크를 찾을 수 없습니다."));
    }

    private void validateProjectMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "프로젝트 멤버가 아닙니다.");
        }
    }
}
