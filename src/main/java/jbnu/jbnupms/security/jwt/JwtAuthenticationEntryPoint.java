package jbnu.jbnupms.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.common.response. ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        CustomException exception = (CustomException) request.getAttribute("exception");
        ErrorCode errorCode = exception != null ? exception.getErrorCode() : ErrorCode.UNAUTHORIZED;

        log.error("Unauthorized error: {}", errorCode.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode,
                exception != null ? exception.getMessage() : errorCode.getMessage(),
                request.getRequestURI()
        );

        CommonResponse<ErrorResponse> commonResponse = CommonResponse.fail(errorResponse);

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(commonResponse));
    }
}