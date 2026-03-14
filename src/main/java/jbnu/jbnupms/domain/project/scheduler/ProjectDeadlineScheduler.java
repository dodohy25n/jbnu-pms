package jbnu.jbnupms.domain.project.scheduler;

import jbnu.jbnupms.domain.notification.event.ProjectDueEvent;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectStatus;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectDeadlineScheduler {

    private final ProjectRepository         projectRepository;
    private final ProjectMemberRepository   projectMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 0 * * *")
    public void checkProjectDeadlines() {
        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        notifyDueReached(today);
        notifyOverdue(yesterday);
    }

    private void notifyDueReached(LocalDate date) {
        List<Project> projects = projectRepository.findProjectsDueInRange(
                date.atStartOfDay(), date.atTime(LocalTime.MAX), ProjectStatus.DONE);

        log.info("[Scheduler] 프로젝트 마감 도달 {}건 ({})", projects.size(), date);

        for (Project project : projects) {
            List<Long> memberIds = projectMemberRepository.findByProjectId(project.getId())
                    .stream()
                    .map(pm -> pm.getUser().getId())
                    .collect(Collectors.toList());

            if (!memberIds.isEmpty()) {
                eventPublisher.publishEvent(
                        new ProjectDueEvent(project.getId(), project.getName(), memberIds, false));
            }
        }
    }

    private void notifyOverdue(LocalDate date) {
        List<Project> projects = projectRepository.findProjectsDueInRange(
                date.atStartOfDay(), date.atTime(LocalTime.MAX), ProjectStatus.DONE);

        log.info("[Scheduler] 프로젝트 마감 초과 {}건 ({})", projects.size(), date);

        for (Project project : projects) {
            List<Long> memberIds = projectMemberRepository.findByProjectId(project.getId())
                    .stream()
                    .map(pm -> pm.getUser().getId())
                    .collect(Collectors.toList());

            if (!memberIds.isEmpty()) {
                eventPublisher.publishEvent(
                        new ProjectDueEvent(project.getId(), project.getName(), memberIds, true));
            }
        }
    }
}