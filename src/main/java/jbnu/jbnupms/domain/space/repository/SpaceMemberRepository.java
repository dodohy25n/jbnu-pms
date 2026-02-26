package jbnu.jbnupms.domain.space.repository;

import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.entity.SpaceMember;
import jbnu.jbnupms.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {
    List<SpaceMember> findBySpaceId(Long spaceId);

    List<SpaceMember> findByUserId(Long userId);

    boolean existsBySpaceAndUser(Space space, User user);

    Optional<SpaceMember> findByUserIdAndSpaceId(Long userId, Long spaceId);

    boolean existsBySpaceIdAndUserId(Long spaceId, Long userId);
}