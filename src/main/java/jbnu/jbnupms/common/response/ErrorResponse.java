package jbnu.jbnupms.common.response;

import jbnu.jbnupms.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ErrorResponse {
    private final LocalDateTime timestamp; // 에러 발생 시각
    private final String path; // 요청 경로
    private final String code; // 에러 코드
    private final String message; // 에러 메시지
    private final List<ErrorDetail> details; // 검증 에러용 (일반 에러에선 null)

    // 일반 예외
    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(path)
                .code(errorCode.name())
                .message(message)
                .details(null)
                .build();
    }

    // Validation 예외
    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(path)
                .code(errorCode.name())
                .message(errorCode.getMessage()) // "입력값 유효성 검사에 실패했습니다."
                .details(ErrorDetail.of(bindingResult)) // BindingResult -> List<ErrorDetail> 변환
                .build();
    }

    @Getter
    @Builder
    public static class ErrorDetail {
        private final String field;
        private final String value;
        private final String reason;

        // BindingResult의 에러 목록을 Stream으로 순회하며 변환
        public static List<ErrorDetail> of(BindingResult bindingResult) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            return fieldErrors.stream()
                    .map(error -> ErrorDetail.builder()
                            .field(error.getField())
                            .value(error.getRejectedValue() == null ? "" : error.getRejectedValue().toString()) // 사용자가 입력한 잘못된 값
                            .reason(error.getDefaultMessage()) // @NotBlank(message="...")의 메시지
                            .build())
                    .collect(Collectors.toList());
        }
    }
}
