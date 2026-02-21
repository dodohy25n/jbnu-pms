package jbnu.jbnupms.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(example = "user@jbnu.ac.kr")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(
            regexp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$",
            message = "올바른 이메일 형식이 아닙니다."
    )
    private String email;

    @Schema(example = "password1!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}