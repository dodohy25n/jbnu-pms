package jbnu.jbnupms.domain.task.scheduler;

import jbnu.jbnupms.domain.space.entity.ActionType;
import jbnu.jbnupms.domain.space.service.ActivityLogService;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskStatus;
import jbnu.jbnupms.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDeadlineScheduler {

    private final TaskRepository taskRepository;
    private final ActivityLogService activityLogService;

    /**
     * 매일 자정에 실행
     * - 오늘 마감인 미완료 태스크 → TASK_DUE_REACHED (마감 도달)
     * - 어제 마감이 지난 미완료 태스크 → TASK_OVERDUE (마감 지연)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void checkTaskDeadlines() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        logDueReached(today);
        logOverdue(yesterday);
    }

    // 오늘 마감 도달 태스크 로깅
    private void logDueReached(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Task> tasks = taskRepository.findTasksDueInRange(start, end, TaskStatus.DONE);
        log.info("[Scheduler] 마감 도달 태스크 {}건 감지 ({})", tasks.size(), date);

        for (Task task : tasks) {
            activityLogService.logSystemActivity(
                    task.getProject().getSpace(),
                    task.getProject().getId(),
                    task.getProject().getName(),
                    task.getId(),
                    task.getTitle(),
                    ActionType.TASK_DUE_REACHED,
                    "'" + task.getTitle() + "' 작업의 마감일이 도래했습니다.");
        }
    }

    // 어제 마감이 지나 지연된 태스크 로깅
    private void logOverdue(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Task> tasks = taskRepository.findTasksDueInRange(start, end, TaskStatus.DONE);
        log.info("[Scheduler] 마감 지연 태스크 {}건 감지 ({})", tasks.size(), date);

        for (Task task : tasks) {
            activityLogService.logSystemActivity(
                    task.getProject().getSpace(),
                    task.getProject().getId(),
                    task.getProject().getName(),
                    task.getId(),
                    task.getTitle(),
                    ActionType.TASK_OVERDUE,
                    "'" + task.getTitle() + "' 작업이 마감일을 초과했습니다.");
        }
    }
}
