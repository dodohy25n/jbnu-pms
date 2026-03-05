package jbnu.jbnupms.domain.user.service;

import jbnu.jbnupms.common.audit.UserAuditLogger;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.user.dto.*;
import jbnu.jbnupms.domain.user.entity.RefreshToken;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.entity.VerificationType;
import jbnu.jbnupms.domain.user.repository.RefreshTokenRepository;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import jbnu.jbnupms.security.jwt.JwtTokenProvider;
import jbnu.jbnupms.security.oauth.OAuth2UserInfoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserAuditLogger auditLogger;
    @Mock private OAuth2UserInfoService oauth2UserInfoService;
    @Mock private VerificationService verificationService;
    @Mock private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("회원가입 성공")
    void register_success() {
        // RegisterRequest는 @AllArgsConstructor 있음 (email, verificationCode, password, name 순)
        RegisterRequest request = new RegisterRequest("test@test.com", "123456", "pass1234", "홍길동");

        User savedUser = User.builder()
                .email("test@test.com").password("encoded").name("홍길동").provider("EMAIL").build();

        doNothing().when(verificationService).validateVerification(anyString(), anyString(), any());
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("pass1234")).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        Long result = authService.register(request);

        assertThat(result).isEqualTo(savedUser.getId());
        verify(verificationService).deleteVerificationCode("test@test.com", VerificationType.REGISTER);
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_fail_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("dup@test.com", "123456", "pass1234", "홍길동");

        doNothing().when(verificationService).validateVerification(anyString(), anyString(), any());
        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // LoginRequest는 @AllArgsConstructor 있음 (email, password 순)
        LoginRequest request = new LoginRequest("test@test.com", "pass1234");

        User user = User.builder()
                .email("test@test.com").password("encoded").name("홍길동").provider("EMAIL").build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pass1234", "encoded")).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(any(), anyString())).willReturn("access-token");
        given(refreshTokenRepository.findValidTokenByUserId(any(), any())).willReturn(Optional.of(
                RefreshToken.builder().userId(user.getId()).token("refresh-token")
                        .expiresAt(LocalDateTime.now().plusDays(7)).build()
        ));

        TokenResponse result = authService.login(request);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 없음")
    void login_fail_emailNotFound() {
        LoginRequest request = new LoginRequest("no@test.com", "pass1234");

        given(userRepository.findByEmail("no@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_invalidPassword() {
        LoginRequest request = new LoginRequest("test@test.com", "wrong");

        User user = User.builder()
                .email("test@test.com").password("encoded").name("홍길동").provider("EMAIL").build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refresh_success() {
        // RefreshTokenRequest는 @AllArgsConstructor 있음
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh");

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(1L).token("valid-refresh").expiresAt(LocalDateTime.now().plusDays(1)).build();

        User user = User.builder()
                .email("test@test.com").password("encoded").name("홍길동").provider("EMAIL").build();

        given(refreshTokenRepository.findValidTokenByToken(anyString(), any())).willReturn(Optional.of(refreshToken));
        given(userRepository.findById(any())).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateAccessToken(any(), anyString())).willReturn("new-access-token");

        TokenResponse result = authService.refresh(request);

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("valid-refresh");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 리프레시 토큰")
    void refresh_fail_expiredToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("expired-refresh");

        given(refreshTokenRepository.findValidTokenByToken(anyString(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("이메일 사용 가능")
    void checkEmail_available() {
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        EmailCheckResponse result = authService.checkEmailAvailability("new@test.com");
        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("이메일 사용 불가 - 중복")
    void checkEmail_unavailable() {
        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);
        EmailCheckResponse result = authService.checkEmailAvailability("dup@test.com");
        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("비밀번호 재설정 성공")
    void resetPassword_success() {
        // ResetPasswordRequest는 @AllArgsConstructor 있음 (email, code, newPassword 순)
        ResetPasswordRequest request = new ResetPasswordRequest("test@test.com", "123456", "newpass1234");

        User user = User.builder()
                .email("test@test.com").password("oldEncoded").name("홍길동").provider("EMAIL").build();

        doNothing().when(verificationService).validateVerification(anyString(), anyString(), any());
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newpass1234")).willReturn("newEncoded");
        given(userRepository.save(any())).willReturn(user);

        authService.resetPassword(request);

        verify(verificationService).deleteVerificationCode("test@test.com", VerificationType.PASSWORD_RESET);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 소셜 로그인 사용자")
    void resetPassword_fail_socialUser() {
        ResetPasswordRequest request = new ResetPasswordRequest("social@test.com", "123456", "newpass1234");

        User socialUser = User.builder()
                .email("social@test.com").password(null).name("소셜유저").provider("GOOGLE").build();

        doNothing().when(verificationService).validateVerification(anyString(), anyString(), any());
        given(userRepository.findByEmail("social@test.com")).willReturn(Optional.of(socialUser));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOCIAL_USER_PASSWORD_CHANGE);
    }
}