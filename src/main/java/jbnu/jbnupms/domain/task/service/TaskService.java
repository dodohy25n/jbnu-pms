package jbnu.jbnupms.domain.task.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.notification.event.TaskAssignedEvent;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.task.dto.TaskCreateRequest;
import jbnu.jbnupms.domain.task.dto.TaskResponse;
import jbnu.jbnupms.domain.task.dto.TaskUpdateRequest;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskAssignee;
import jbnu.jbnupms.domain.task.entity.TaskAssigneeRole;
import jbnu.jbnupms.domain.task.repository.TaskAssigneeRepository;
import jbnu.jbnupms.domain.task.repository.TaskRepository;
import jbnu.jbnupms.domain.task.dto.TaskSummaryDto;
import jbnu.jbnupms.domain.task.entity.TaskStatus;
import jbnu.jbnupms.domain.space.repository.SpaceMemberRepository;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import jbnu.jbnupms.domain.space.entity.ActionType;
import jbnu.jbnupms.domain.space.service.ActivityLogService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import jbnu.jbnupms.domain.task.dto.MyTaskSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final SpaceMemberRepository spaceMemberRepository;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;

    // 태스크 생성
    @Transactional
    public Long createTask(Long userId, TaskCreateRequest request) {
        User user = this.getUser(userId);
        Project project = this.getProject(request.getProjectId());

        // 프로젝트 멤버인지 확인 (생성은 쓰기 권한 필요)
        this.validateProjectWriteAccess(project.getId(), user.getId());

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

        // 담당자 할당 (ASSIGNEE 역할)
        for (Long assigneeId : request.getAssigneeIds()) {
            assignUserToTask(task, assigneeId, TaskAssigneeRole.ASSIGNEE);
            if (!assigneeId.equals(userId)) {
                eventPublisher.publishEvent(new TaskAssignedEvent(
                        task.getId(), task.getTitle(),
                        assigneeId, user.getName(), project.getId()));
            }
        }
        // 관리자 할당 (MANAGER 역할)
        if (request.getManagerIds() != null) {
            for (Long managerId : request.getManagerIds()) {
                assignUserToTask(task, managerId, TaskAssigneeRole.MANAGER);
                if (!managerId.equals(userId)) {
                    eventPublisher.publishEvent(new TaskAssignedEvent(
                            task.getId(), task.getTitle(),
                            managerId, user.getName(), project.getId()));
                }
            }
        }

        return task.getId();
    }

    // 프로젝트별 태스크 목록 조회 (계층형)
    public List<TaskResponse> getTasks(Long userId, Long projectId) {

        // 프로젝트 읽기 권한 확인 (public 프로젝트는 스페이스 멤버도 조회 가능)
        this.validateProjectReadAccess(projectId, userId);

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
        this.validateProjectReadAccess(projectId, userId);

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
        this.validateProjectWriteAccess(task.getProject().getId(), userId);

        TaskStatus oldStatus = task.getStatus();

        task.update(
                request.getTitle() != null ? request.getTitle() : task.getTitle(),
                request.getDescription() != null ? request.getDescription() : task.getDescription(),
                request.getStatus() != null ? request.getStatus() : task.getStatus(),
                request.getPriority() != null ? request.getPriority() : task.getPriority(),
                request.getDueDate() != null ? request.getDueDate() : task.getDueDate(),
                request.getProgress() != null ? request.getProgress() : task.getProgress());

        if (request.getStatus() != null && oldStatus != TaskStatus.DONE && task.getStatus() == TaskStatus.DONE) {
            activityLogService.logActivity(task.getProject().getSpace(), task.getProject().getId(),
                    task.getProject().getName(), task.getId(), task.getTitle(), ActionType.TASK_COMPLETED,
                    getUser(userId), "작업이 완료되었습니다.");
        }
    }

    // 태스크 삭제
    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = this.getTaskById(taskId);
        this.validateProjectWriteAccess(task.getProject().getId(), userId);

        taskRepository.delete(task);
    }

    // 담당자 추가
    @Transactional
    public void addAssignee(Long userId, Long taskId, Long assigneeId, TaskAssigneeRole role) {
        Task task = getTaskById(taskId);
        validateProjectWriteAccess(task.getProject().getId(), userId);

        assignUserToTask(task, assigneeId, role != null ? role : TaskAssigneeRole.ASSIGNEE);

        User assignee = getUser(assigneeId);
        activityLogService.logActivity(
                task.getProject().getSpace(), task.getProject().getId(),
                task.getProject().getName(), task.getId(), task.getTitle(),
                ActionType.ASSIGNEE_CHANGED, getUser(userId),
                assignee.getName() + "님이 담당자로 추가되었습니다.");

        // 알림 이벤트 publish
        eventPublisher.publishEvent(new TaskAssignedEvent(
                task.getId(), task.getTitle(),
                assigneeId, getUser(userId).getName(),
                task.getProject().getId()));
    }

    // 담당자 삭제
    @Transactional
    public void removeAssignee(Long userId, Long taskId, Long assigneeId) {
        Task task = this.getTaskById(taskId);
        this.validateProjectWriteAccess(task.getProject().getId(), userId);

        User assignee = this.getUser(assigneeId);

        taskAssigneeRepository.deleteByTaskAndUser(task, assignee);
        activityLogService.logActivity(task.getProject().getSpace(), task.getProject().getId(),
                task.getProject().getName(), task.getId(), task.getTitle(), ActionType.ASSIGNEE_CHANGED,
                getUser(userId), assignee.getName() + "님이 담당자에서 제외되었습니다.");
    }

    private void assignUserToTask(Task task, Long assigneeId, TaskAssigneeRole role) {
        User assignee = this.getUser(assigneeId);
        // 담당자/관리자도 프로젝트 멤버여야 함
        if (!projectMemberRepository.existsByProjectIdAndUserId(task.getProject().getId(), assigneeId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "담당자가 프로젝트 멤버가 아닙니다.");
        }

        if (!taskAssigneeRepository.existsByTaskAndUser(task, assignee)) {
            taskAssigneeRepository.save(TaskAssignee.builder()
                    .task(task)
                    .user(assignee)
                    .role(role)
                    .build());
        }
    }

    // 긴급 작업 목록 (마감일 오늘까지 거나 이미 지난 작업 중 완료되지 않은 작업)
    public List<TaskSummaryDto> getUrgentTasks(Long userId, Long spaceId) {
        validateSpaceMember(userId, spaceId);
        LocalDateTime endOfToday = LocalDateTime.now().with(LocalTime.MAX);
        return taskAssigneeRepository
                .findUrgentTasksByUserId(userId, spaceId, TaskStatus.DONE,
                        endOfToday, PageRequest.of(0, 5))
                .stream()
                .map(ta -> TaskSummaryDto.from(ta.getTask(),
                        TaskSummaryDto.AssigneeSummaryDto.builder()
                                .userId(ta.getUser().getId())
                                .userName(ta.getUser().getName())
                                .profileImage(ta.getUser().getProfileImage())
                                .build()))
                .collect(Collectors.toList());
    }

    // 내 작업 목록 (범위별 페이징)
    public Page<TaskSummaryDto> getMyTasks(Long userId, Long spaceId,
                                           TaskStatus status, String range,
                                           Pageable pageable) {
        validateSpaceMember(userId, spaceId);
        LocalDateTime startDate = null;
        LocalDateTime endDate   = null;
        LocalDateTime now       = LocalDateTime.now();
        if ("TODAY".equalsIgnoreCase(range)) {
            startDate = now.toLocalDate().atStartOfDay();
            endDate   = now.with(LocalTime.MAX);
        } else if ("WEEK".equalsIgnoreCase(range)) {
            int dayOfWeek = now.getDayOfWeek().getValue();
            startDate = now.minusDays(dayOfWeek - 1).toLocalDate().atStartOfDay();
            endDate   = now.plusDays(7 - dayOfWeek).with(LocalTime.MAX);
        }
        return taskAssigneeRepository
                .findMyTasksByUserIdAndSpaceId(userId, spaceId, status, startDate, endDate, pageable)
                .map(ta -> TaskSummaryDto.from(ta.getTask(),
                        TaskSummaryDto.AssigneeSummaryDto.builder()
                                .userId(ta.getUser().getId())
                                .userName(ta.getUser().getName())
                                .profileImage(ta.getUser().getProfileImage())
                                .build()));
    }

    // 내 작업 요약 (상태별 개수)
    public MyTaskSummaryDto getMyTaskSummary(Long userId, Long spaceId) {
        validateSpaceMember(userId, spaceId);
        long totalCount      = taskAssigneeRepository.countByUserIdAndSpaceId(userId, spaceId);
        long inProgressCount = taskAssigneeRepository.countByUserIdAndSpaceIdAndStatus(
                userId, spaceId, TaskStatus.IN_PROGRESS);
        long doneCount       = taskAssigneeRepository.countByUserIdAndSpaceIdAndStatus(
                userId, spaceId, TaskStatus.DONE);
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        long delayedCount = taskAssigneeRepository.countDelayedTasks(
                userId, spaceId, TaskStatus.DONE, startOfToday);
        return MyTaskSummaryDto.builder()
                .totalCount(totalCount).inProgressCount(inProgressCount)
                .doneCount(doneCount).delayedCount(delayedCount)
                .build();
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

    // 읽기 권한: 프로젝트 멤버 OR (public 프로젝트 + 스페이스 멤버)
    private void validateProjectReadAccess(Long projectId, Long userId) {
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            return;
        }
        Project project = this.getProject(projectId);
        if (project.getIsPublic() && spaceMemberRepository.existsBySpaceIdAndUserId(project.getSpace().getId(), userId)) {
            return;
        }
        throw new CustomException(ErrorCode.ACCESS_DENIED, "프로젝트에 접근 권한이 없습니다.");
    }

    // 쓰기 권한: 프로젝트 멤버이면서 VIEWER가 아닌 경우
    private void validateProjectWriteAccess(Long projectId, Long userId) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCESS_DENIED, "프로젝트 멤버가 아닙니다."));
        if (member.getRole() == ProjectRole.VIEWER) {
            throw new CustomException(ErrorCode.VIEWER_WRITE_ACCESS_DENIED);
        }
    }

    private void validateSpaceMember(Long userId, Long spaceId) {
        if (!spaceMemberRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 스페이스의 멤버가 아닙니다.");
        }
    }
}
