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
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.service.ActivityLogService;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskPriority;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks private CommentService commentService;
    @Mock private CommentRepository commentRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ActivityLogService activityLogService;

    private User buildUser(String name) {
        return User.builder().email(name + "@test.com").password("pw").name(name).provider("EMAIL").build();
    }

    private Task buildTask(User creator) {
        Space space = Space.builder().name("스페이스").description("설명").owner(creator).build();
        Project project = Project.builder().space(space).name("프로젝트").description("설명").build();
        Task task = Task.builder().project(project).creator(creator).title("태스크")
                .description("설명").priority(TaskPriority.MEDIUM).dueDate(LocalDateTime.now().plusDays(1)).build();
        // task.id null이면 CommentService 66번줄 parent.getTask().getId().equals(taskId) NPE
        injectId(task, 1L);
        return task;
    }

    private void injectId(Object obj, Long id) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(obj, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private CommentCreateRequest commentCreateRequest(Long taskId, String content, Long parentId) {
        try {
            CommentCreateRequest req = new CommentCreateRequest();
            setField(req, "taskId", taskId);
            setField(req, "content", content);
            setField(req, "parentId", parentId);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    @DisplayName("댓글 생성 성공")
    void createComment_success() {
        User user = buildUser("홍길동");
        Task task = buildTask(user);
        Comment comment = Comment.builder().task(task).user(user).content("댓글 내용").build();
        ProjectMember member = ProjectMember.builder()
                .project(task.getProject()).user(user).role(ProjectRole.MEMBER).build();

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(projectMemberRepository.findByProjectIdAndUserId(any(), any())).willReturn(Optional.of(member));
        given(commentRepository.save(any())).willReturn(comment);

        CommentResponse result = commentService.createComment(commentCreateRequest(1L, "댓글 내용", null), 1L);

        assertThat(result.getContent()).isEqualTo("댓글 내용");
        verify(activityLogService).logActivity(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("댓글 생성 실패 - 프로젝트 멤버 아님")
    void createComment_fail_notProjectMember() {
        User user = buildUser("홍길동");
        Task task = buildTask(user);

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(false);

        assertThatThrownBy(() -> commentService.createComment(commentCreateRequest(1L, "댓글", null), 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TASK_ACCESS_DENIED);
    }

    @Test
    @DisplayName("댓글 생성 실패 - VIEWER는 댓글 불가")
    void createComment_fail_viewer() {
        User user = buildUser("홍길동");
        Task task = buildTask(user);
        ProjectMember viewer = ProjectMember.builder()
                .project(task.getProject()).user(user).role(ProjectRole.VIEWER).build();

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(projectMemberRepository.findByProjectIdAndUserId(any(), any())).willReturn(Optional.of(viewer));

        assertThatThrownBy(() -> commentService.createComment(commentCreateRequest(1L, "댓글", null), 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VIEWER_WRITE_ACCESS_DENIED);
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 1depth 초과")
    void createComment_fail_depthExceeded() {
        User user = buildUser("홍길동");
        Task task = buildTask(user);
        Comment grandParent = Comment.builder().task(task).user(user).content("부모").build();
        Comment parent = Comment.builder().task(task).user(user).parent(grandParent).content("대댓글").build();
        ProjectMember member = ProjectMember.builder()
                .project(task.getProject()).user(user).role(ProjectRole.MEMBER).build();

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(projectMemberRepository.findByProjectIdAndUserId(any(), any())).willReturn(Optional.of(member));
        given(commentRepository.findActiveById(99L)).willReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.createComment(commentCreateRequest(1L, "3depth", 99L), 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_DEPTH_EXCEEDED);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 없음")
    void updateComment_fail_notFound() {
        given(commentRepository.findByIdWithTask(99L)).willReturn(Optional.empty());

        // CommentUpdateRequest는 @AllArgsConstructor 있음
        assertThatThrownBy(() -> commentService.updateComment(99L, new CommentUpdateRequest("수정"), 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글 없음")
    void deleteComment_fail_notFound() {
        given(commentRepository.findByIdWithTask(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(99L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 목록 조회 성공")
    void getCommentsByTask_success() {
        User user = buildUser("홍길동");
        Task task = buildTask(user);
        Comment comment = Comment.builder().task(task).user(user).content("댓글").build();

        given(taskRepository.findById(1L)).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(any(), any())).willReturn(true);
        given(commentRepository.findParentCommentsByTaskId(1L)).willReturn(List.of(comment));
        given(commentRepository.findRepliesByParentId(any())).willReturn(List.of());

        List<CommentResponse> result = commentService.getCommentsByTask(1L, 1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 태스크 없음")
    void getCommentsByTask_fail_taskNotFound() {
        given(taskRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentsByTask(99L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TASK_NOT_FOUND);
    }
}