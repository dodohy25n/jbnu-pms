package jbnu.jbnupms.domain.user.service;

import jbnu.jbnupms.common.audit.UserAuditLogger;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.user.dto.*;
import jbnu.jbnupms.domain.user.entity.VerificationType;
import jbnu.jbnupms.security.jwt.JwtTokenProvider;
import jbnu.jbnupms.domain.user.entity.RefreshToken;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.RefreshTokenRepository;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import jbnu.jbnupms.security.oauth.OAuth2UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAuditLogger auditLogger;
    private final OAuth2UserInfoService oauth2UserInfoService;
    private final VerificationService verificationService;

    @Transactional
    public Long register(RegisterRequest request) {
        // 인증 코드 검증
        verificationService.validateVerification(
                request.getEmail(),
                request.getVerificationCode(),
                VerificationType.REGISTER
        );

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 빌더 패턴 (엔티티 전체를 호출하지 않기 위해)
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .provider("EMAIL")
                .build();

        User savedUser = userRepository.save(user);

        // 인증 코드 삭제
        verificationService.deleteVerificationCode(request.getEmail(), VerificationType.REGISTER);

        // 감사 로그 기록
        // todo (1): (예정) 레벨을 나눠서 원인 별로 로깅 기록 추가 (ip에서 로그인 시도/실패 기록) - 어노테이션 활용
        auditLogger.logRegister(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getProvider()
        );

        return savedUser.getId();
    }

    // [V] email 중복 확인 조건문 제거
    // [V] 에러코드 반환 시에 아이디/패스워드 중 어떤게 틀렸는지 구분 필요
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 이메일/패스워드 불일치 구분
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // Access Token은 항상 새로 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());

        // Refresh Token 처리 (고정 만료 방식)
        String refreshToken = handleRefreshToken(user.getId());

        return TokenResponse.of(accessToken, refreshToken);
    }

    // [V] 액세스 토큰 만료시간 5분으로 수정
    // todo (6) (예정) : Redis 블랙리스트로 access token 무효화 구현하여 로그아웃
    //  -> accesstoken이 만료되지 않은 상황에서 계속 재사용 될 수 있는 상황
    @Transactional
    public void logout(Long userId) {
        log.info("User logged out: userId={}", userId);
    }

    // 트랜잭션 범위 최적화 - HTTP 호출은 트랜잭션 밖에서 수행
    public TokenResponse oauth2Login(OAuth2LoginRequest request) {
        // 1. 트랜잭션 밖에서 OAuth provider로부터 사용자 정보 가져오기 (HTTP 호출)
        Map<String, Object> userInfo = oauth2UserInfoService.getUserInfo(
                request.getProvider(),
                request.getAccessToken()
        );

        // 2. 사용자 정보 추출
        String providerId = (String) userInfo.get("sub");
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String profileImage = (String) userInfo.get("picture");

        if (providerId == null || email == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN,
                    "OAuth 토큰으로부터 필수 정보를 가져올 수 없습니다.");
        }

        // 3. 트랜잭션 안에서 DB 작업만 수행
        User user = saveOrUpdateOAuthUserInTransaction(
                providerId,
                email,
                name,
                profileImage,
                request.getProvider().toUpperCase()
        );

        // 4. JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = handleRefreshToken(user.getId());

        // 민감 정보 로깅 제거
        log.info("OAuth2 login successful: provider={}, userId={}", request.getProvider(), user.getId());

        return TokenResponse.of(accessToken, refreshToken);
    }

    // [V] refresh token 전달 방식 (string-> request 객체)
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        // [V] : db단에서 쿼리문으로 조회하도록 수정 (만료 확인 로직 제거)
        // (EXPIRED_REFRESH_TOKEN 에러로 리프레시 토큰 만료가 확인되면 프론트에서 로그인 페이지로 유도합니다)
        RefreshToken refreshToken = refreshTokenRepository
                .findValidTokenByToken(request.getRefreshToken(), LocalDateTime.now())
                .orElseThrow(() -> new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN));

        // refresh token에 이메일이 포함되지 않으므로 access token 생성에 필요한 이메일을 User로 조회
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // access token만 새로 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());

        return TokenResponse.of(newAccessToken, refreshToken.getToken());
    }

    /**
     * 이메일 중복 확인
     */
    public EmailCheckResponse checkEmailAvailability(String email) {
        boolean exists = userRepository.existsByEmail(email);

        if (exists) {
            log.info("Email already exists: {}", email);
            return EmailCheckResponse.unavailable();
        }

        log.info("Email available: {}", email);
        return EmailCheckResponse.available();
    }

    /**
     * 비밀번호 재설정
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. 인증 코드 검증
        verificationService.validateVerification(
                request.getEmail(),
                request.getCode(),
                VerificationType.PASSWORD_RESET
        );

        // 2. 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 소셜 로그인 사용자는 비밀번호 변경 불가
        if (!user.getProvider().contains("EMAIL")) {
            throw new CustomException(ErrorCode.SOCIAL_USER_PASSWORD_CHANGE);
        }

        // 4. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.updatePassword(encodedPassword);
        userRepository.save(user);

        // 5. 인증 코드 삭제
        verificationService.deleteVerificationCode(request.getEmail(), VerificationType.PASSWORD_RESET);

        // 6. 감사 로그 기록
        auditLogger.logChangePassword(user.getId());

        log.info("Password reset successfully: email={}", request.getEmail());
    }

    /**
     * OAuth 사용자 저장/업데이트는 별도 트랜잭션으로 분리
     */
    @Transactional
    protected User saveOrUpdateOAuthUserInTransaction(String providerId, String email, String name,
                                                      String profileImage, String provider) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existingUser -> {
                    existingUser.updateName(name);
                    if (profileImage != null) {
                        existingUser.updateProfileImage(profileImage);
                    }
                    log.info("OAuth2 user updated: email={}", email);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {
                                String currentProvider = existingUser.getProvider();
                                if (!currentProvider.contains(provider)) {
                                    existingUser.setProvider(currentProvider + "," + provider);
                                }
                                existingUser.setProviderId(providerId);
                                if (profileImage != null) {
                                    existingUser.updateProfileImage(profileImage);
                                }
                                User updated = userRepository.save(existingUser);
                                log.info("Added {} to existing user: email={}", provider, email);
                                return updated;
                            })
                            .orElseGet(() -> {
                                User newUser = User.builder()
                                        .email(email)
                                        .name(name)
                                        .profileImage(profileImage)
                                        .provider(provider)
                                        .providerId(providerId)
                                        .password(null)
                                        .build();

                                User saved = userRepository.save(newUser);

                                auditLogger.logRegister(
                                        saved.getId(),
                                        saved.getEmail(),
                                        saved.getName(),
                                        saved.getProvider()
                                );

                                log.info("New OAuth2 user created: email={}", email);
                                return saved;
                            });
                });
    }

    /**
     * Refresh Token 처리 (고정 만료 방식 - 7일)
     * - 기존 토큰 유효하면 재사용
     * - 없거나 만료되었으면 새로 생성
     */
    // [V] 토큰이 만료된 경우와 토큰이 처음 생성되는 경우 모두를 포함하여 UPSERT 쿼리 하나로 처리
    // -> UPSERT 적용을 위해 refresh token의 user_id 필드에 unique 조건이 추가됨
    private String handleRefreshToken(Long userId) {
        return refreshTokenRepository
                .findValidTokenByUserId(userId, LocalDateTime.now())
                .map(RefreshToken::getToken)
                .orElseGet(() -> {
                    String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

                    // UPSERT: 있으면 UPDATE, 없으면 INSERT (한 번의 쿼리)
                    refreshTokenRepository.upsertRefreshToken(
                            userId,
                            newRefreshToken,
                            LocalDateTime.now().plusDays(7),
                            LocalDateTime.now()
                    );

                    return newRefreshToken;
                });
    }
}