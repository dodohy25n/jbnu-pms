package jbnu.jbnupms.domain.comment.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.comment.dto.CommentCreateRequest;
import jbnu.jbnupms.domain.comment.dto.CommentResponse;
import jbnu.jbnupms.domain.comment.dto.CommentUpdateRequest;
import jbnu.jbnupms.domain.comment.entity.Comment;
import jbnu.jbnupms.domain.comment.repository.CommentRepository;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.repository.SpaceRepository;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskPriority;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestConfig.class)
class CommentServiceIntegrationTest {

    @Autowired private CommentService commentService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private EntityManager em;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("test@test.com").password("pw").name("홍길동").provider("EMAIL").build());

        Space space = spaceRepository.save(Space.builder()
                .name("스페이스").description("설명").owner(user).build());

        Project project = projectRepository.save(Project.builder()
                .space(space).name("프로젝트").description("설명").build());

        projectMemberRepository.save(ProjectMember.builder()
                .project(project).user(user).role(ProjectRole.MEMBER).build());

        task = taskRepository.save(Task.builder()
                .project(project).creator(user).title("태스크")
                .priority(TaskPriority.MEDIUM).dueDate(LocalDateTime.now().plusDays(1)).build());
    }

    @Test
    @DisplayName("댓글 생성 후 조회 성공")
    void createAndGetComments() {
        // CommentCreateRequest(taskId, parentId, content)
        CommentCreateRequest request = new CommentCreateRequest(task.getId(), null, "테스트 댓글");

        CommentResponse created = commentService.createComment(request, user.getId());
        em.flush();
        em.clear();

        List<CommentResponse> comments = commentService.getCommentsByTask(task.getId(), user.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getContent()).isEqualTo("테스트 댓글");
    }

    @Test
    @DisplayName("대댓글 생성 성공")
    void createReply_success() {
        Comment parent = commentRepository.save(Comment.builder()
                .task(task).user(user).content("부모 댓글").build());
        em.flush();
        em.clear();

        CommentCreateRequest request = new CommentCreateRequest(task.getId(), parent.getId(), "대댓글");

        CommentResponse reply = commentService.createComment(request, user.getId());

        assertThat(reply.getContent()).isEqualTo("대댓글");
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_success() {
        Comment comment = commentRepository.save(Comment.builder()
                .task(task).user(user).content("원본").build());
        em.flush();
        em.clear();

        // CommentUpdateRequest(content)
        CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");

        CommentResponse result = commentService.updateComment(comment.getId(), request, user.getId());

        assertThat(result.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("댓글 수정 실패 - 작성자 아님")
    void updateComment_fail_notAuthor() {
        User other = userRepository.save(User.builder()
                .email("other@test.com").password("pw").name("다른사람").provider("EMAIL").build());
        Comment comment = commentRepository.save(Comment.builder()
                .task(task).user(user).content("원본").build());
        em.flush();
        em.clear();

        CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");

        assertThatThrownBy(() -> commentService.updateComment(comment.getId(), request, other.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_UNAUTHORIZED);
    }

    @Test
    @DisplayName("대댓글 있는 댓글 삭제 불가")
    void deleteComment_fail_hasReplies() {
        Comment parent = commentRepository.save(Comment.builder()
                .task(task).user(user).content("부모").build());
        commentRepository.save(Comment.builder()
                .task(task).user(user).parent(parent).content("대댓글").build());
        em.flush();
        em.clear();

        assertThatThrownBy(() -> commentService.deleteComment(parent.getId(), user.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_HAS_REPLIES);
    }

    @Test
    @DisplayName("댓글 소프트 삭제 후 조회 시 제외된다")
    void deleteComment_softDelete() {
        Comment comment = commentRepository.save(Comment.builder()
                .task(task).user(user).content("삭제될 댓글").build());
        em.flush();
        em.clear();

        commentService.deleteComment(comment.getId(), user.getId());
        em.flush();
        em.clear();

        // isDeleted=true인 댓글은 findParentCommentsByTaskId에서 제외되어야 함
        List<CommentResponse> comments = commentService.getCommentsByTask(task.getId(), user.getId());
        assertThat(comments).isEmpty();
    }
}