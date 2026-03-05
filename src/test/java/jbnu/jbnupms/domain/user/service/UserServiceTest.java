package jbnu.jbnupms.domain.user.service;

import jbnu.jbnupms.common.audit.UserAuditLogger;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.user.dto.UpdateUserRequest;
import jbnu.jbnupms.domain.user.dto.UserResponse;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.RefreshTokenRepository;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import jbnu.jbnupms.domain.user.repository.WithdrawnUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private WithdrawnUserRepository withdrawnUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserAuditLogger auditLogger;

    private User buildEmailUser(String name) {
        return User.builder().email("test@test.com").password("encoded").name(name).provider("EMAIL").build();
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_success() {
        User user = buildEmailUser("홍길동");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.getMyInfo(1L);
        assertThat(response.getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 탈퇴한 사용자")
    void getMyInfo_fail_deletedUser() {
        User user = buildEmailUser("홍길동");
        user.softDelete();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getMyInfo(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_DELETED);
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - 이름 변경")
    void updateUser_success_nameChange() {
        User user = buildEmailUser("홍길동");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any())).willReturn(user);

        // UpdateUserRequest는 @AllArgsConstructor 있음 (name, currentPassword, password, position 순)
        UpdateUserRequest request = new UpdateUserRequest("김철수", null, null, null);

        UserResponse response = userService.updateUser(1L, 1L, request);
        assertThat(response.getName()).isEqualTo("김철수");
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 본인이 아님")
    void updateUser_fail_notSelf() {
        UpdateUserRequest request = new UpdateUserRequest("김철수", null, null, null);

        assertThatThrownBy(() -> userService.updateUser(1L, 2L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 소셜 로그인 사용자 비밀번호 변경 시도")
    void updateUser_fail_socialUserPasswordChange() {
        User socialUser = User.builder()
                .email("social@test.com").password(null).name("소셜유저").provider("GOOGLE").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(socialUser));

        UpdateUserRequest request = new UpdateUserRequest(null, null, "newpass12", null);

        assertThatThrownBy(() -> userService.updateUser(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOCIAL_USER_PASSWORD_CHANGE);
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 현재 비밀번호 불일치")
    void updateUser_fail_wrongCurrentPassword() {
        User user = buildEmailUser("홍길동");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongCurrent", "encoded")).willReturn(false);

        UpdateUserRequest request = new UpdateUserRequest(null, "wrongCurrent", "newpass12", null);

        assertThatThrownBy(() -> userService.updateUser(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteUser_success() {
        User user = buildEmailUser("홍길동");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any())).willReturn(user);

        userService.deleteUser(1L, 1L, "개인 사정");

        verify(withdrawnUserRepository).save(any());
        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 본인이 아님")
    void deleteUser_fail_notSelf() {
        assertThatThrownBy(() -> userService.deleteUser(1L, 2L, "사유"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }
}