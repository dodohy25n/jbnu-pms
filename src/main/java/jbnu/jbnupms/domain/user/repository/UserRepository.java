package jbnu.jbnupms.domain.user.repository;

import jbnu.jbnupms.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회 (로그인) - 삭제되지 않은 사용자만
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false")
    Optional<User> findByEmail(String email);

    // 이메일 중복 체크 (회원가입) - 삭제되지 않은 사용자만
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.isDeleted = false")
    boolean existsByEmail(String email);

    // 소셜 로그인용 - 삭제되지 않은 사용자만
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.isDeleted = false")
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // ID로 사용자 조회 - 삭제되지 않은 사용자만
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false")
    Optional<User> findById(Long id);

    // 모든 사용자 조회 - 삭제된 사용자 포함
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdIncludingDeleted(Long id);
}