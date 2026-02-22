package jbnu.jbnupms.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jbnu.jbnupms.domain.user.entity.VerificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeRequest {

    @Schema(example = "user@jbnu.ac.kr")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(
            regexp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$",
            message = "올바른 이메일 형식이 아닙니다."
    )
    private String email;

    @Schema(example = "123456")
    @NotBlank(message = "인증 코드는 필수입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
    private String code;

    @Schema(example = "REGISTER")
    @NotNull(message = "인증 타입은 필수입니다.")
    private VerificationType type;
}