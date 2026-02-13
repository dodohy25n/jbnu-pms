package jbnu.jbnupms.domain.task.repository;

import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskAssignee;
import jbnu.jbnupms.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, Long> {
    
    List<TaskAssignee> findByTaskId(Long taskId);

    List<TaskAssignee> findAllByTask_ProjectId(Long projectId);
    
    Optional<TaskAssignee> findByTaskAndUser(Task task, User user);
    
    boolean existsByTaskAndUser(Task task, User user);
    
    void deleteByTaskAndUser(Task task, User user);
}
