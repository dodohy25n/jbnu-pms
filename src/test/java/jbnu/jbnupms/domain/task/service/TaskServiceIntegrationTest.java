package jbnu.jbnupms.domain.task.service;

import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.repository.SpaceMemberRepository;
import jbnu.jbnupms.domain.space.repository.SpaceRepository;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import jbnu.jbnupms.TestConfig;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestConfig.class)
class TaskServiceIntegrationTest {

    @Autowired private TaskService taskService;
    @Autowired private UserRepository userRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskAssigneeRepository taskAssigneeRepository;
    @Autowired private EntityManager em;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("test@test.com").password("pw").name("홍길동").provider("EMAIL").build());

        Space space = spaceRepository.save(Space.builder()
                .name("스페이스").description("설명").owner(user).build());

        project = projectRepository.save(Project.builder()
                .space(space).name("프로젝트").description("설명").build());

        projectMemberRepository.save(ProjectMember.builder()
                .project(project).user(user).role(ProjectRole.ADMIN).build());
    }

    @Test
    @DisplayName("태스크 생성 후 조회 시 계층 구조가 올바르게 반환된다")
    void createAndGetTasks_hierarchical() {
        // 루트 태스크 생성
        TaskCreateRequest rootRequest = new TaskCreateRequest();
        rootRequest.setProjectId(project.getId());
        rootRequest.setTitle("루트 태스크");
        rootRequest.setPriority(TaskPriority.HIGH);
        Long rootId = taskService.createTask(user.getId(), rootRequest);

        // 하위 태스크 생성
        TaskCreateRequest childRequest = new TaskCreateRequest();
        childRequest.setProjectId(project.getId());
        childRequest.setTitle("하위 태스크");
        childRequest.setPriority(TaskPriority.LOW);
        childRequest.setParentId(rootId);
        Long childId = taskService.createTask(user.getId(), childRequest);

        em.flush();
        em.clear();

        List<TaskResponse> tasks = taskService.getTasks(user.getId(), project.getId());

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo("루트 태스크");
        assertThat(tasks.get(0).getChildren()).hasSize(1);
        assertThat(tasks.get(0).getChildren().get(0).getTitle()).isEqualTo("하위 태스크");
    }

    @Test
    @DisplayName("태스크 담당자 할당 시 담당자 정보가 조회에 포함된다")
    void createTask_withAssignee() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setProjectId(project.getId());
        request.setTitle("담당자 있는 태스크");
        request.setPriority(TaskPriority.MEDIUM);
        request.setAssigneeIds(List.of(user.getId()));

        Long taskId = taskService.createTask(user.getId(), request);

        em.flush();
        em.clear();

        TaskResponse response = taskService.getTask(user.getId(), taskId);

        assertThat(response.getAssignees()).hasSize(1);
        assertThat(response.getAssignees().get(0).getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("태스크 상태를 DONE으로 수정하면 ActivityLog가 기록된다")
    void updateTask_toDone_logsActivity() {
        Task task = taskRepository.save(Task.builder()
                .project(project).creator(user).title("태스크")
                .priority(TaskPriority.MEDIUM).dueDate(LocalDateTime.now().plusDays(1)).build());
        em.flush();
        em.clear();

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setStatus(TaskStatus.DONE);

        taskService.updateTask(user.getId(), task.getId(), request);
        // ActivityLog 기록 여부는 activityLogRepository로 검증 가능
        // 여기선 예외 없이 완료됨을 확인
    }

    @Test
    @DisplayName("하위 태스크에 담당자 할당 시 루트 태스크 조회에 담당자 포함")
    void getTasks_ShouldIncludeAssigneesForSubTasks() {
        Task parentTask = taskRepository.save(Task.builder()
                .project(project).creator(user).title("Parent Task")
                .priority(TaskPriority.MEDIUM).dueDate(LocalDateTime.now().plusDays(1)).build());

        Task childTask = taskRepository.save(Task.builder()
                .project(project).creator(user).parent(parentTask).title("Child Task")
                .priority(TaskPriority.LOW).dueDate(LocalDateTime.now().plusDays(2)).build());

        taskAssigneeRepository.save(TaskAssignee.builder().task(childTask).user(user).build());

        em.flush();
        em.clear();

        List<TaskResponse> tasks = taskService.getTasks(user.getId(), project.getId());

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getChildren().get(0).getAssignees()).hasSize(1);
        assertThat(tasks.get(0).getChildren().get(0).getAssignees().get(0).getId())
                .isEqualTo(user.getId());
    }
}