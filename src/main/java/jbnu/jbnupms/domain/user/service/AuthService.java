package jbnu.jbnupms.domain.user.service;

import jbnu.jbnupms.common.audit.UserAuditLogger;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.security.jwt.JwtTokenProvider;
import jbnu.jbnupms.domain.user.dto.LoginRequest;
import jbnu.jbnupms.domain.user.dto.RegisterRequest;
import jbnu.jbnupms.domain.user.dto.TokenResponse;
import jbnu.jbnupms.domain.user.entity.RefreshToken;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.RefreshTokenRepository;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import jbnu.jbnupms.domain.user.repository.WithdrawnUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final WithdrawnUserRepository withdrawnUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAuditLogger auditLogger;

    @Transactional
    public Long register(RegisterRequest request) {
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

        // 감사 로그 기록
        // todo 레벨을 나눠서 원인 별로 로깅 기록 추가 (ip에서 로그인 시도/실패 기록) - 어노테이션 활용
        auditLogger.logRegister(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getProvider()
        );

        return savedUser.getId();
    }

    // todo: email 중복 조회를 쿼리 조회로 최적화 할 지
    // todo: 쿼리로 처리한다면 이메일 중복확인 조건문이 필요없음 -> 이미 삭제된 사용자를 에러코드로 반환할 필요 없음
    // todo: 에러코드 반환 시에 아이디/패스워드 중 어떤게 틀렸는지 구분 필요
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getIsDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Access Token은 항상 새로 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());

        // Refresh Token 처리 (고정 만료 방식)
        String refreshToken = handleRefreshToken(user.getId());

        return TokenResponse.of(accessToken, refreshToken);
    }

    // todo: refresh를 통째로 전달하도록 수정
    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        // todo: db단에서 쿼리문으로 조회하도록 수정
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // todo: refresh 토큰 만료 로직 분리
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        // todo: 에러코드 반환하지 않고 데이터베이스 에러로 전달되도록 수정
        // 리프레시 과정 이전에 확인된다면 리프레시에서 다시 확인할 필요 없음
        User user = userRepository.findActiveById(refreshToken.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Access Token만 새로 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());

        return TokenResponse.of(newAccessToken, refreshToken.getToken());
    }

    // todo: (로그아웃) - accesstoken이 만료되지 않은 상황에서 계속 재사용 될 수 있음 -> 제한하는 코드를 서치할 것
    // 만료시간: 5분 수정
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Refresh Token 처리 (고정 만료 방식 - 7일)
     * 1. 기존 토큰이 있고 유효하면 재사용
     * 2. 없거나 만료되었으면 새로 생성
     */
    private String handleRefreshToken(Long userId) {
        return refreshTokenRepository.findByUserId(userId)
                .filter(token -> !token.isExpired())  // 만료 안 된 것만
                .map(existingToken -> {
                    // 기존 토큰 유지 (만료 시간 변경 안 함)
                    log.info("Reusing existing refresh token for userId: {} (expires at: {})",
                            userId, existingToken.getExpiresAt());
                    return existingToken.getToken();
                })
                .orElseGet(() -> {
                    // 새 Refresh Token 생성
                    String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

                    refreshTokenRepository.findByUserId(userId)
                            .ifPresent(refreshTokenRepository::delete);

                    RefreshToken refreshToken = RefreshToken.builder()
                            .userId(userId)
                            .token(newRefreshToken)
                            .expiresAt(LocalDateTime.now().plusDays(7))
                            .build();
                    refreshTokenRepository.save(refreshToken);

                    return newRefreshToken;
                });
    }
}