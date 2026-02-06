package jbnu.jbnupms.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jbnu.jbnupms.domain.user.dto.TokenResponse;
import jbnu.jbnupms.domain.user.entity.RefreshToken;
import jbnu.jbnupms.domain.user.repository.RefreshTokenRepository;
import jbnu.jbnupms.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Long userId = ((Number) oAuth2User.getAttributes().get("userId")).longValue();
        String email = (String) oAuth2User.getAttributes().get("email");

        log.info("OAuth2 login success - userId: {}, email: {}", userId, email);

        // Access Token은 항상 새로 생성
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email);

        // Refresh Token 처리 (고정 만료 방식 - 7일)
        String refreshTokenValue = handleRefreshToken(userId);

        // JSON 응답
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshTokenValue);
        response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));

        // 응답 완료 후 추가 처리 방지
        response.getWriter().flush();
        clearAuthenticationAttributes(request);
    }

    /**
     * Refresh Token 처리 (고정 만료 방식 - 7일)
     * - 기존 토큰이 있고 유효하면 재사용
     * - 없거나 만료되었으면 새로 생성
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

                    // 기존 만료된 토큰 삭제 (있다면)
                    refreshTokenRepository.findByUserId(userId)
                            .ifPresent(refreshTokenRepository::delete);

                    // 새 토큰 저장
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