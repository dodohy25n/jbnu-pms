package jbnu.jbnupms.domain.space.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateSpaceRequest {
    private String name;
    private String description;
    private Long ownerId; // 임시. 추후 로그인된 유저의 ID로 수정
}
