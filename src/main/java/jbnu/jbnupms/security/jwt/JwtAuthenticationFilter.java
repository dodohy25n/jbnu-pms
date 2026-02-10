package jbnu.jbnupms.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(String.valueOf(userId));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (CustomException e) {
            // =================== 액세스 토큰 만료 =====================
            // (액세스토큰 만료) 0. validateToken 예외로 EXPIRED_ACCESS_TOKEN 던져짐
            // catch 후 filterChain.doFilter() 진행
            log.error("JWT authentication failed: {}", e.getMessage());

            // Access Token 만료 시, Refresh Token도 확인
            if (e.getErrorCode() == ErrorCode.EXPIRED_ACCESS_TOKEN) {
                try {
                    // Authorization 헤더에서 userId 추출 (만료된 토큰이어도 파싱 가능)
                    String token = extractTokenFromRequest(request);
                    Long userId = jwtTokenProvider.getUserIdFromTokenWithoutValidation(token);

                    // Refresh Token 유효성 확인
                    boolean hasValidRefreshToken = refreshTokenRepository
                            .findValidTokenByUserId(userId, LocalDateTime.now())
                            .isPresent();

                    if (!hasValidRefreshToken) {
                        // Refresh Token도 만료됨 -> 재로그인 유도 EXPIRED_REFRESH_TOKEN
                        request.setAttribute("exception",
                                new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN));
                    } else {
                        // Refresh Token은 유효함 -> Access Token만 만료
                        request.setAttribute("exception", e);
                    }
                } catch (Exception ex) {
                    // 파싱 실패 시 원래 예외 사용
                    request.setAttribute("exception", e);
                }
            } else {
                request.setAttribute("exception", e);
            }
        }
        // (액세스토큰 만료) 1. VirtualFilterChain.doFilter() 호출 (필터 순서를 바꾸며 차례대로 실행)
        /* Spring Security 내부 (FilterChainProxy)
         List<Filter> filters = [
         JwtAuthenticationFilter,                  // 0번
                 UsernamePasswordAuthenticationFilter,     // 1번
                 AnonymousAuthenticationFilter,            // 2번
                 ExceptionTranslationFilter,               // 3번
                 FilterSecurityInterceptor                 // 4번 (여기서 인증 확인)
         */
        // (액세스토큰 만료) 2. (4)FilterSecurityInterceptor에서 SecurityContext 확인 후 AuthenticationException 던짐
        // (액세스토큰 만료) 3. (3)ExceptionTranslationFilter에서 AuthenticationException 잡은 후 authenticationEntryPoint.commence 호출
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}