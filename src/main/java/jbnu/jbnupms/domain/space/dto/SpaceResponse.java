package jbnu.jbnupms.domain.space.dto;

import jbnu.jbnupms.domain.space.entity.Space;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@Builder
public class SpaceResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public static SpaceResponse from(Space space) {
        return SpaceResponse.builder()
                .id(space.getId())
                .name(space.getName())
                .description(space.getDescription())
                .createdAt(space.getCreatedAt())
                .build();
    }
}