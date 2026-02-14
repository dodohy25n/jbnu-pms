package jbnu.jbnupms.domain.comment.repository;

import jbnu.jbnupms.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Task의 모든 댓글 조회 (부모 댓글만, 삭제되지 않은 것)
    @Query("SELECT c FROM Comment c WHERE c.task.id = :taskId AND c.parent IS NULL AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> findParentCommentsByTaskId(@Param("taskId") Long taskId);

    // 특정 댓글의 대댓글 조회 (삭제되지 않은 것)
    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    // Task의 모든 댓글 수 (대댓글 포함, 삭제되지 않은 것)
    long countByTaskIdAndIsDeletedFalse(Long taskId);

    // 특정 댓글 조회 (task 포함, 권한 검증용, 삭제되지 않은 것)
    @Query("SELECT c FROM Comment c JOIN FETCH c.task WHERE c.id = :commentId AND c.isDeleted = false")
    Optional<Comment> findByIdWithTask(@Param("commentId") Long commentId);

    // 댓글 ID로 조회 (삭제되지 않은 것)
    @Query("SELECT c FROM Comment c WHERE c.id = :commentId AND c.isDeleted = false")
    Optional<Comment> findActiveById(@Param("commentId") Long commentId);

    // 사용자가 작성한 댓글 조회 (삭제되지 않은 것)
    List<Comment> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
}