package jbnu.jbnupms.domain.task.service;

import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.repository.SpaceRepository;
import jbnu.jbnupms.domain.task.dto.TaskResponse;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskAssignee;
import jbnu.jbnupms.domain.task.entity.TaskPriority;
import jbnu.jbnupms.domain.task.repository.TaskAssigneeRepository;
import jbnu.jbnupms.domain.task.repository.TaskRepository;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskAssigneeRepository taskAssigneeRepository;

    @Autowired
    private jakarta.persistence.EntityManager em;

    @Test
    @DisplayName("하위 작업 조회 시 담당자 정보가 포함되어야 한다")
    void getTasks_ShouldIncludeAssigneesForSubTasks() {
        // Given
        User user = userRepository.save(User.builder()
                .email("test@example.com")
                .password("password")
                .name("Test User")
                .provider("EMAIL")
                .build());

        Space space = spaceRepository.save(Space.builder()
                .name("Test Space")
                .description("Description")
                .owner(user)
                .build());

        Project project = projectRepository.save(Project.builder()
                .space(space)
                .name("Test Project")
                .description("Description")
                .build());

        projectMemberRepository.save(ProjectMember.builder()
                .project(project)
                .user(user)
                .role(ProjectRole.MEMBER)
                .build());

        Task parentTask = taskRepository.save(Task.builder()
                .project(project)
                .creator(user)
                .title("Parent Task")
                .description("Parent Description")
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build());

        Task childTask = taskRepository.save(Task.builder()
                .project(project)
                .creator(user)
                .parent(parentTask)
                .title("Child Task")
                .description("Child Description")
                .priority(TaskPriority.LOW)
                .dueDate(LocalDateTime.now().plusDays(2))
                .build());

        // Assign user to child task
        taskAssigneeRepository.save(TaskAssignee.builder()
                .task(childTask)
                .user(user)
                .build());
        
        // Force flush and clear to ensure data is fetched from DB
        em.flush();
        em.clear();

        // When
        List<TaskResponse> tasks = taskService.getTasks(user.getId(), project.getId());

        // Then
        assertThat(tasks).hasSize(1);
        TaskResponse parentResponse = tasks.get(0);
        assertThat(parentResponse.getId()).isEqualTo(parentTask.getId());
        
        List<TaskResponse> children = parentResponse.getChildren();
        assertThat(children).hasSize(1);
        
        TaskResponse childResponse = children.get(0);
        assertThat(childResponse.getId()).isEqualTo(childTask.getId());
        assertThat(childResponse.getAssignees()).isNotEmpty();
        assertThat(childResponse.getAssignees().get(0).getId()).isEqualTo(user.getId());
    }
}
