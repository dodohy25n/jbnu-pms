package jbnu.jbnupms.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 일반 에러
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CommonResponse<ErrorResponse>> handleCustomException(CustomException e,
                                                                               HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("[CustomException] url: {} | errorType: {} | message: {}", request.getRequestURI(),
                errorCode.name(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode,
                e.getMessage(),
                request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(errorResponse));
    }

    // @RequestBody 검증 에러 (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<ErrorResponse>> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

        log.info("[ValidationException] url: {} | message: {}", request.getRequestURI(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode,
                e.getBindingResult(),
                request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(errorResponse));
    }

    // @RequestParam 검증 에러 (@Validated + @Email, @Pattern 등)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<ErrorResponse>> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request) {

        ErrorCode errorCode = ErrorCode.INVALID_EMAIL_FORMAT;

        log.info("[ConstraintViolationException] url: {} | message: {}", request.getRequestURI(), e.getMessage());

        List<ErrorResponse.ErrorDetail> details = e.getConstraintViolations().stream()
                .map(v -> ErrorResponse.ErrorDetail.builder()
                        .field(v.getPropertyPath().toString())
                        .value(v.getInvalidValue() == null ? "" : v.getInvalidValue().toString())
                        .reason(v.getMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code(errorCode.name())
                .message(errorCode.getMessage())
                .details(details)
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(errorResponse));
    }

    // 인증 실패 에러
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResponse<ErrorResponse>> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request) {

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        log.info("[AuthenticationException] url: {} | message: {}", request.getRequestURI(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode,
                e.getMessage(),
                request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(errorResponse));
    }

    // 권한 없음 에러
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResponse<ErrorResponse>> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request) {

        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        log.info("[AccessDeniedException] url: {} | message: {}", request.getRequestURI(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode,
                e.getMessage(),
                request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(errorResponse));
    }

    // 405 에러
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<ErrorResponse>> handleMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {

        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;

        log.info("[MethodNotAllowed] url: {} | message: {}", request.getRequestURI(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(errorCode, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(CommonResponse.fail(errorResponse));
    }

    // 415 에러
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<CommonResponse<ErrorResponse>> handleMediaTypeNotSupportedException(
            org.springframework.web.HttpMediaTypeNotSupportedException e,
            HttpServletRequest request) {

        ErrorCode errorCode = ErrorCode.UNSUPPORTED_MEDIA_TYPE;

        log.info("[UnsupportedMediaType] url: {} | message: {}", request.getRequestURI(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(errorCode, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(CommonResponse.fail(errorResponse));
    }

    // 나머지 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<ErrorResponse>> handleAllException(
            Exception e,
            HttpServletRequest request) {

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        log.warn("[InternalServerError] url: {} | message: {}", request.getRequestURI(), e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode,
                e.getMessage(),
                request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(CommonResponse.fail(errorResponse));
    }
}