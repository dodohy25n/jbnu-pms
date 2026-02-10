package jbnu.jbnupms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class CommonResponse<T> {

    private static final Boolean SUCCESS = true;
    private static final Boolean FAIL = false;

    private Boolean isSuccess;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    // 성공 응답 생성자
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(SUCCESS, data);
    }

    // 실패 응답 생성자
    public static <T> CommonResponse<T> fail(T data) {
        return new CommonResponse<>(FAIL, data);
    }

    protected CommonResponse(Boolean isSuccess, T data) {
        this.isSuccess = isSuccess;
        this.data = data;
    }
}
