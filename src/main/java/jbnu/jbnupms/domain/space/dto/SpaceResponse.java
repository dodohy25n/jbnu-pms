package jbnu.jbnupms.domain.space.dto;

import jbnu.jbnupms.domain.space.entity.Space;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SpaceResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private LocalDateTime createdAt;

    public SpaceResponse(Space space) {
        this.id = space.getId();
        this.name = space.getName();
        this.description = space.getDescription();
        this.ownerId = space.getOwner().getId();
        this.createdAt = space.getCreatedAt();
    }
}
