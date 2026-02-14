package jbnu.jbnupms.domain.comment.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.comment.dto.CommentCreateRequest;
import jbnu.jbnupms.domain.comment.dto.CommentResponse;
import jbnu.jbnupms.domain.comment.dto.CommentUpdateRequest;
import jbnu.jbnupms.domain.comment.entity.Comment;
import jbnu.jbnupms.domain.comment.repository.CommentRepository;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.repository.TaskAssigneeRepository;
import jbnu.jbnupms.domain.task.repository.TaskRepository;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * 댓글 생성
     */
    @Transactional
    public CommentResponse createComment(CommentCreateRequest request, Long userId) {
        // Task 존재 확인
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Task 접근 권한 확인 (Task의 Project에 속한 멤버인지)
        validateTaskAccess(task, userId);

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findActiveById(request.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

            // 부모 댓글이 같은 Task에 속하는지 확인
            if (!parent.getTask().getId().equals(request.getTaskId())) {
                throw new CustomException(ErrorCode.COMMENT_TASK_MISMATCH);
            }

            // 대댓글의 대댓글 방지 (1 depth만 허용)
            if (parent.getParent() != null) {
                throw new CustomException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
            }
        }

        Comment comment = Comment.builder()
                .task(task)
                .user(user)
                .parent(parent)
                .content(request.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("댓글 생성 완료: commentId={}, taskId={}, userId={}", savedComment.getId(), task.getId(), userId);

        return CommentResponse.from(savedComment);
    }

    /**
     * Task의 댓글 목록 조회 (대댓글 포함, 계층 구조)
     */
    public List<CommentResponse> getCommentsByTask(Long taskId, Long userId) {
        // Task 존재 확인
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        // Task 접근 권한 확인
        validateTaskAccess(task, userId);

        // 부모 댓글 조회
        List<Comment> parentComments = commentRepository.findParentCommentsByTaskId(taskId);

        // 각 부모 댓글의 대댓글 조회
        Map<Long, List<CommentResponse>> repliesMap = new HashMap<>();
        for (Comment parent : parentComments) {
            List<Comment> replies = commentRepository.findRepliesByParentId(parent.getId());
            List<CommentResponse> replyResponses = replies.stream()
                    .map(CommentResponse::from)
                    .collect(Collectors.toList());
            repliesMap.put(parent.getId(), replyResponses);
        }

        // 부모 댓글에 대댓글 추가
        return parentComments.stream()
                .map(parent -> {
                    CommentResponse response = CommentResponse.from(parent);
                    List<CommentResponse> replies = repliesMap.get(parent.getId());
                    if (replies != null) {
                        replies.forEach(response::addReply);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Long userId) {
        Comment comment = commentRepository.findByIdWithTask(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 작성자 본인 확인
        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.COMMENT_UNAUTHORIZED);
        }

        comment.updateContent(request.getContent());
        log.info("댓글 수정 완료: commentId={}, userId={}", commentId, userId);

        return CommentResponse.from(comment);
    }

    /**
     * 댓글 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findByIdWithTask(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 작성자 본인 확인
        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.COMMENT_UNAUTHORIZED);
        }

        // 부모 댓글인 경우, 대댓글이 있는지 확인
        if (!comment.isReply()) {
            List<Comment> replies = commentRepository.findRepliesByParentId(commentId);
            if (!replies.isEmpty()) {
                throw new CustomException(ErrorCode.COMMENT_HAS_REPLIES);
            }
        }

        // Soft Delete
        comment.softDelete();
        commentRepository.save(comment);

        log.info("댓글 삭제 완료: commentId={}, userId={}", commentId, userId);
    }

    /**
     * Task 접근 권한 확인
     * Task가 속한 Project의 멤버인지 확인
     */
    private void validateTaskAccess(Task task, Long userId) {
        // Task 담당자인지 확인
        boolean isAssignee = taskAssigneeRepository.existsByTaskAndUser(task,
                userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)));

        if (!isAssignee) {
            // Task 담당자가 아니면 Project 멤버인지 확인
            boolean isProjectMember = projectMemberRepository.existsByProjectIdAndUserId(
                    task.getProject().getId(), userId);
            if (!isProjectMember) {
                throw new CustomException(ErrorCode.TASK_ACCESS_DENIED);
            }
        }
    }
}