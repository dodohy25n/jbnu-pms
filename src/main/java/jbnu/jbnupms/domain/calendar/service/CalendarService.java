package jbnu.jbnupms.domain.calendar.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.calendar.dto.CalendarItemDto;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.space.repository.SpaceMemberRepository;
import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.repository.TaskAssigneeRepository;
import jbnu.jbnupms.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final SpaceMemberRepository spaceMemberRepository;

    /**
     * 캘린더 조회
     * - spaceId가 있으면 해당 스페이스만, 없으면 내가 속한 전체 스페이스 대상
     * - year/month 기준으로 마감일이 해당 월인 프로젝트 + 태스크 반환
     */
    public List<CalendarItemDto> getCalendarItems(Long userId, Long spaceId, int year, int month) {
        List<Long> spaceIds = resolveSpaceIds(userId, spaceId);

        LocalDateTime startDate = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime endDate = LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.lastDayOfMonth())
                .atTime(LocalTime.MAX);

        List<Project> projects = projectRepository.findProjectsDueInMonth(spaceIds, startDate, endDate);
        List<Task> tasks = taskRepository.findTasksDueInMonth(spaceIds, startDate, endDate);

        // 내 담당 여부를 배치로 판별 (N+1 방지)
        Set<Long> myProjectIds = resolveMyProjectIds(userId, projects);
        Set<Long> myTaskIds = resolveMyTaskIds(userId, tasks);

        List<CalendarItemDto> items = new ArrayList<>();

        for (Project project : projects) {
            items.add(CalendarItemDto.fromProject(project, myProjectIds.contains(project.getId())));
        }
        for (Task task : tasks) {
            items.add(CalendarItemDto.fromTask(task, myTaskIds.contains(task.getId())));
        }

        items.sort(Comparator.comparing(CalendarItemDto::getDueDate));
        return items;
    }

    // spaceId가 주어지면 멤버 검증 후 단일 스페이스, 없으면 내가 속한 전체 스페이스
    private List<Long> resolveSpaceIds(Long userId, Long spaceId) {
        if (spaceId != null) {
            if (!spaceMemberRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
                throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 스페이스의 멤버가 아닙니다.");
            }
            return List.of(spaceId);
        }
        List<Long> spaceIds = spaceMemberRepository.findByUserIdWithSpace(userId)
                .stream()
                .map(sm -> sm.getSpace().getId())
                .collect(Collectors.toList());
        if (spaceIds.isEmpty()) {
            return List.of();
        }
        return spaceIds;
    }

    // 프로젝트 목록에서 내가 멤버인 프로젝트 ID 집합 (배치 조회)
    private Set<Long> resolveMyProjectIds(Long userId, List<Project> projects) {
        if (projects.isEmpty()) return Set.of();
        List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        return projectMemberRepository.findByProjectIdInAndUserId(projectIds, userId)
                .stream()
                .map(pm -> pm.getProject().getId())
                .collect(Collectors.toSet());
    }

    // 태스크 목록에서 내가 담당자인 태스크 ID 집합 (배치 조회)
    private Set<Long> resolveMyTaskIds(Long userId, List<Task> tasks) {
        if (tasks.isEmpty()) return Set.of();
        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
        return taskAssigneeRepository.findByTaskIdInAndUserId(taskIds, userId)
                .stream()
                .map(ta -> ta.getTask().getId())
                .collect(Collectors.toSet());
    }
}
