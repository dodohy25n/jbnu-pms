package jbnu.jbnupms.domain.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "내 작업 요약 응답 DTO")
public class MyTaskSummaryDto {
    @Schema(description = "전체 작업 수", example = "10")
    private long totalCount;

    @Schema(description = "진행 중 작업 수", example = "4")
    private long inProgressCount;

    @Schema(description = "완료 작업 수", example = "5")
    private long doneCount;

    @Schema(description = "지연 작업 수", example = "1")
    private long delayedCount;
}
