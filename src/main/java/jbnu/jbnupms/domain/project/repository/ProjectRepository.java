package jbnu.jbnupms.domain.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jbnu.jbnupms.domain.project.entity.Project;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

        // 스페이스 내 프로젝트 목록 조회
        @Query("SELECT p FROM Project p WHERE p.space.id = :spaceId")
        List<Project> findBySpaceId(Long spaceId);
}
