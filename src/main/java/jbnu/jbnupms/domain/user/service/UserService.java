package jbnu.jbnupms.domain.user.service;

import jbnu.jbnupms.common.audit.UserAuditLogger;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.domain.user.dto.UpdateUserRequest;
import jbnu.jbnupms.domain.user.dto.UserResponse;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.entity.WithdrawnUser;
import jbnu.jbnupms.domain.user.repository.RefreshTokenRepository;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import jbnu.jbnupms.domain.user.repository.WithdrawnUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final WithdrawnUserRepository withdrawnUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditLogger auditLogger;

    public UserResponse getMyInfo(Long userId) {
        User user = findActiveUserById(userId);
        return UserResponse.from(user);
    }

    public UserResponse getUserById(Long userId) {
        User user = findActiveUserById(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long requestUserId, Long targetUserId, UpdateUserRequest request) {
        // 본인만 수정 가능
        if (!requestUserId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        User user = findActiveUserById(targetUserId);

        // 변경 전 데이터 저장 (감사 로그용)
        String oldName = user.getName();
        String oldProfileImage = user.getProfileImage();

        // 이름 업데이트
        if (request.getName() != null && !request.getName().isBlank()) {
            user.updateName(request.getName());
        }

        // 비밀번호 업데이트
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            // 소셜 로그인 사용자는 비밀번호 변경 불가
            if (!user.getProvider().contains("EMAIL")) {
                throw new CustomException(ErrorCode.SOCIAL_USER_PASSWORD_CHANGE);
            }

            String encodedPassword = passwordEncoder.encode(request.getPassword());
            user.updatePassword(encodedPassword);

            // 비밀번호 변경 로그
            auditLogger.logChangePassword(user.getId());
        }

        userRepository.save(user);

        // 프로필 업데이트 로그
        if (!oldName.equals(user.getName()) ||
                (oldProfileImage != null && !oldProfileImage.equals(user.getProfileImage()))) {
            auditLogger.logUpdateProfile(
                    requestUserId,
                    targetUserId,
                    oldName,
                    user.getName(),
                    oldProfileImage,
                    user.getProfileImage()
            );
        }

        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long requestUserId, Long targetUserId, String reason) {
        // 본인만 탈퇴 가능
        if (!requestUserId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        User user = findActiveUserById(targetUserId);

        // 탈퇴 전 원본 이메일 저장 (감사 로그용)
        String originalEmail = user.getEmail();

        // 1. withdrawn_users 테이블에 정보 저장 (원본 이메일)
        WithdrawnUser withdrawnUser = WithdrawnUser.builder()
                .email(originalEmail)
                .reason(reason)
                .build();
        withdrawnUserRepository.save(withdrawnUser);

        // 2. users 테이블의 사용자 정보를 ghost 유저로 변환
        convertToGhost(user);
        userRepository.save(user);

        // 3. 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUserId(targetUserId);

        // 4. 감사 로그 기록 (원본 이메일로)
        auditLogger.logDelete(requestUserId, targetUserId, originalEmail, reason);

        log.info("User converted to ghost: userId={}, originalEmail={}, ghostEmail={}",
                targetUserId, originalEmail, user.getEmail());
    }

    /** 회원 탈퇴 시 사용자 정보를 ghost 유저로 변환
    * email: deleted{userId}@ghost.local (재가입 가능하도록 중복 방지)
    * name: "탈퇴한 사용자" (UI에 표시될 이름)
    * provider: "DELETED" (탈퇴 상태 표시)
    * isDeleted: true
    * (else: null)
    */
    private void convertToGhost(User user) {
        user.updateEmail("deleted" + user.getId() + "@ghost.local");
        user.updatePassword(null);
        user.updateName("탈퇴한 사용자");
        user.updateProfileImage(null);
        user.setProviderId(null);
        user.setProvider("DELETED");
        user.softDelete();
    }

    private User findActiveUserById(Long userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        return user;
    }
}