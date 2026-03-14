package jbnu.jbnupms.domain.space.repository;

import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.entity.SpaceMember;
import jbnu.jbnupms.domain.space.entity.SpaceRole;
import jbnu.jbnupms.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {
    List<SpaceMember> findBySpaceId(Long spaceId);

    List<SpaceMember> findByUserId(Long userId);

    // 캘린더용: 내가 속한 스페이스 목록을 space 정보와 함께 조회 (N+1 방지)
    @Query("SELECT sm FROM SpaceMember sm JOIN FETCH sm.space s WHERE sm.user.id = :userId")
    List<SpaceMember> findByUserIdWithSpace(@Param("userId") Long userId);

    boolean existsBySpaceAndUser(Space space, User user);

    Optional<SpaceMember> findByUserIdAndSpaceId(Long userId, Long spaceId);

    boolean existsBySpaceIdAndUserId(Long spaceId, Long userId);

    long countBySpaceIdAndRole(Long spaceId, SpaceRole role);
}