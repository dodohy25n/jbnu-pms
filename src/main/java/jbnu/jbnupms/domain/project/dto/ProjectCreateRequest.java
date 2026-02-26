package jbnu.jbnupms.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectCreateRequest {

    @NotNull(message = "Space ID는 필수입니다.")
    private Long spaceId;

    @NotBlank(message = "프로젝트 이름은 필수입니다.")
    private String name;

    private String description;

    private LocalDateTime dueDate;

    private Boolean isPublic;
}
