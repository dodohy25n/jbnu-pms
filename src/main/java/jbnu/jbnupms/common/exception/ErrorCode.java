package jbnu.jbnupms.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."), // Existing
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값 유효성 검사에 실패했습니다."),
    INVALID_QUERY_PARAM(HttpStatus.BAD_REQUEST, "유효하지 않은 쿼리 파라미터입니다."),
    USER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 탈퇴한 사용자입니다."), // Existing
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."), // Existing
    CANNOT_DELETE_SELF(HttpStatus.BAD_REQUEST, "본인 계정은 삭제할 수 없습니다."), // Existing
    OAUTH2_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth2 제공자입니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."), // Existing (Duplicate with TOKEN_EXPIRED? Keeping both for
    // now)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."), // Existing
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다."), // Existing

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."), // Existing (Duplicate with FORBIDDEN)
    SOCIAL_USER_PASSWORD_CHANGE(HttpStatus.FORBIDDEN, "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다."),

    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),

    // 409 Conflict
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "데이터가 이미 존재합니다."),
    STATE_CONFLICT(HttpStatus.CONFLICT, "리소스 상태가 충돌합니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."), // Existing
    OAUTH2_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 다른 방식으로 가입된 이메일입니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않는 HTTP 메서드입니다."),

    // 415 Unsupported Media Type
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),

    // 422 Unprocessable Entity
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "요청은 올바르나 처리할 수 없습니다."),

    // 429 Too Many Requests
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청 횟수가 허용치를 초과했습니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}