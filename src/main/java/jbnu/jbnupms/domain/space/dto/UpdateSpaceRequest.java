package jbnu.jbnupms.domain.space.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateSpaceRequest {
    private String name;
    private String description;
}
