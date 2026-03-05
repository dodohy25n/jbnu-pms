package jbnu.jbnupms.domain.task.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.repository.SpaceMemberRepository;
import jbnu.jbnupms.domain.space.service.ActivityLogService;
import jbnu.jbnupms.domain.task.dto.TaskCreateRequest;
import jbnu.jbnupms.domain.task.dto.TaskResponse;
import jbnu.jbnupms.domain.task.dto.TaskUpdateRequest;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskAssignee;
import jbnu.jbnupms.domain.task.entity.TaskPriority;
import jbnu.jbnupms.domain.task.entity.TaskStatus;
import jbnu.jbnupms.domain.task.repository.TaskAssigneeRepository;
import jbnu.jbnupms.domain.task.repository.TaskRepository;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceMockTest {

    @InjectMocks
    private TaskService taskService;

    @Mock private TaskRepository taskRepository;
    @Mock private TaskAssigneeRepository taskAssigneeRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private SpaceMemberRepository spaceMemberRepository;
    @Mock private ActivityLogService activityLogService;

    private User buildUser(String name) {
        return User.builder().email(name + "@test.com").password("pw").name(name).provider("EMAIL").build();
    }

    private Space buildSpace(User owner) {
        return Space.builder().name("스페이스").description("설명").owner(owner).build();
    }

    private Project buildProject(Space space) {
        return Project.builder().space(space).name("프로젝트").description("설명").build();
    }

    private Task buildTask(Project project, User creator) {
        return Task.builder()
                .project(project)
                .creator(creator)
                .title("태스크")
                .description("설명")
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // createTask
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("태스크 생성 성공")
    void createTask_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        Task task = buildTask(project, user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(taskRepository.save(any())).willReturn(task);

        TaskCreateRequest request = new TaskCreateRequest();
        request.setProjectId(1L);
        request.setTitle("태스크");
        request.setPriority(TaskPriority.MEDIUM);
        request.setDueDate(LocalDateTime.now().plusDays(1));

        Long result = taskService.createTask(1L, request);

        assertThat(result).isEqualTo(task.getId());
    }

    @Test
    @DisplayName("태스크 생성 실패 - 프로젝트 멤버 아님")
    void createTask_fail_notProjectMember() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(false);

        TaskCreateRequest request = new TaskCreateRequest();
        request.setProjectId(1L);
        request.setTitle("태스크");

        assertThatThrownBy(() -> taskService.createTask(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("태스크 생성 실패 - 상위 태스크 없음")
    void createTask_fail_parentNotFound() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(taskRepository.findById(99L)).willReturn(Optional.empty());

        TaskCreateRequest request = new TaskCreateRequest();
        request.setProjectId(1L);
        request.setTitle("태스크");
        request.setParentId(99L);

        assertThatThrownBy(() -> taskService.createTask(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────
    // getTasks
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("프로젝트별 태스크 목록 조회 성공")
    void getTasks_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        Task task = buildTask(project, user);

        given(projectMemberRepository.existsByProjectIdAndUserId(1L, 1L)).willReturn(true);
        given(taskRepository.findRootTasksByProjectId(1L)).willReturn(List.of(task));
        given(taskAssigneeRepository.findAllByTask_ProjectId(1L)).willReturn(List.of());

        List<TaskResponse> result = taskService.getTasks(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("태스크");
    }

    @Test
    @DisplayName("태스크 목록 조회 실패 - 프로젝트 멤버 아님")
    void getTasks_fail_notMember() {
        given(projectMemberRepository.existsByProjectIdAndUserId(1L, 1L)).willReturn(false);

        assertThatThrownBy(() -> taskService.getTasks(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    // ─────────────────────────────────────────────────────────
    // getTask
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("태스크 단건 조회 성공")
    void getTask_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        Task task = buildTask(project, user);

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(taskAssigneeRepository.findAllByTask_ProjectId(any())).willReturn(List.of());

        TaskResponse result = taskService.getTask(1L, 1L);

        assertThat(result.getTitle()).isEqualTo("태스크");
    }

    @Test
    @DisplayName("태스크 단건 조회 실패 - 태스크 없음")
    void getTask_fail_notFound() {
        given(taskRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(1L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────
    // updateTask
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("태스크 수정 성공")
    void updateTask_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        Task task = buildTask(project, user);

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("수정된 태스크");
        request.setStatus(TaskStatus.IN_PROGRESS);

        taskService.updateTask(1L, 1L, request);

        assertThat(task.getTitle()).isEqualTo("수정된 태스크");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("태스크 완료 처리 시 ActivityLog 기록")
    void updateTask_done_logsActivity() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        Task task = buildTask(project, user); // status = NOT_STARTED

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setStatus(TaskStatus.DONE);

        taskService.updateTask(1L, 1L, request);

        verify(activityLogService).logActivity(any(), any(), any(), any(), any(), any(), any(), anyString());
    }

    // ─────────────────────────────────────────────────────────
    // deleteTask
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("태스크 삭제 성공")
    void deleteTask_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        Task task = buildTask(project, user);

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);

        taskService.deleteTask(1L, 1L);

        verify(taskRepository).delete(task);
    }

    // ─────────────────────────────────────────────────────────
    // addAssignee / removeAssignee
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("담당자 추가 성공")
    void addAssignee_success() {
        User user = buildUser("홍길동");
        User assignee = buildUser("담당자");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        Task task = buildTask(project, user);

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.findById(2L)).willReturn(Optional.of(assignee));
        given(taskAssigneeRepository.existsByTaskAndUser(task, assignee)).willReturn(false);

        taskService.addAssignee(1L, 1L, 2L);

        verify(taskAssigneeRepository).save(any());
        verify(activityLogService).logActivity(any(), any(), any(), any(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("담당자 추가 - 이미 담당자면 중복 저장 안 함")
    void addAssignee_skip_ifAlreadyAssigned() {
        User user = buildUser("홍길동");
        User assignee = buildUser("담당자");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        Task task = buildTask(project, user);

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.findById(2L)).willReturn(Optional.of(assignee));
        given(taskAssigneeRepository.existsByTaskAndUser(task, assignee)).willReturn(true);

        taskService.addAssignee(1L, 1L, 2L);

        verify(taskAssigneeRepository, never()).save(any());
    }

    @Test
    @DisplayName("담당자 삭제 성공")
    void removeAssignee_success() {
        User user = buildUser("홍길동");
        User assignee = buildUser("담당자");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        Task task = buildTask(project, user);

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.findById(2L)).willReturn(Optional.of(assignee));

        taskService.removeAssignee(1L, 1L, 2L);

        verify(taskAssigneeRepository).deleteByTaskAndUser(task, assignee);
        verify(activityLogService).logActivity(any(), any(), any(), any(), any(), any(), any(), anyString());
    }
}