package jbnu.jbnupms.domain.project.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.project.dto.ProjectCreateRequest;
import jbnu.jbnupms.domain.project.dto.ProjectInviteRequest;
import jbnu.jbnupms.domain.project.dto.ProjectResponse;
import jbnu.jbnupms.domain.project.dto.ProjectRoleUpdateRequest;
import jbnu.jbnupms.domain.project.dto.ProjectUpdateRequest;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.repository.SpaceMemberRepository;
import jbnu.jbnupms.domain.space.repository.SpaceRepository;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

        private final ProjectRepository projectRepository;
        private final ProjectMemberRepository projectMemberRepository;
        private final SpaceRepository spaceRepository;
        private final SpaceMemberRepository spaceMemberRepository;
        private final UserRepository userRepository;

        // 프로젝트 생성
        @Transactional
        public Long createProject(Long userId, ProjectCreateRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                // Space 존재 확인
                Space space = spaceRepository.findById(request.getSpaceId())
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "스페이스를 찾을 수 없습니다."));

                // Space 멤버인지 확인
                if (!spaceMemberRepository.existsBySpaceAndUser(space, user)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                // 프로젝트 생성
                Project project = Project.builder()
                                .space(space)
                                .name(request.getName())
                                .description(request.getDescription())
                                .build();

                projectRepository.save(project);

                // 생성자를 ADMIN으로 추가
                ProjectMember member = ProjectMember.builder()
                                .project(project)
                                .user(user)
                                .role(ProjectRole.ADMIN)
                                .build();

                projectMemberRepository.save(member);

                return project.getId();
        }

        // 사용자가 속한 특정 스페이스의 프로젝트 목록 조회
        public List<ProjectResponse> getProjects(Long userId, Long spaceId) {
                return projectMemberRepository.findByUserIdAndSpaceId(userId, spaceId).stream()
                                .map(ProjectMember::getProject)
                                .map(ProjectResponse::from)
                                .collect(Collectors.toList());
        }

       
        // 프로젝트 단건 조회
        public ProjectResponse getProject(Long userId, Long projectId) {
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                // 멤버인지 확인
                if (!projectMemberRepository.existsByProjectAndUser(project, user)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                return ProjectResponse.from(project);
        }

        // 프로젝트 수정
        @Transactional
        public void updateProject(Long userId, Long projectId, ProjectUpdateRequest request) {
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                validateLeaderPermission(userId, projectId);

                project.update(request.getName(), request.getDescription());
        }

        // 프로젝트 삭제
        @Transactional
        public void deleteProject(Long userId, Long projectId) {
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                validateLeaderPermission(userId, projectId);

                projectRepository.delete(project);
        }

        // 프로젝트 멤버 초대
        @Transactional
        public void inviteMember(Long userId, Long projectId, ProjectInviteRequest request) {
                
                // 리더인지 확인
                validateLeaderPermission(userId, projectId);

                User targetUser = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "해당 프로젝트를 찾을 수 없습니다."));

                // 이미 멤버인지 확인
                if (projectMemberRepository.existsByProjectIdAndUserId(projectId, targetUser.getId())) {
                        throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 참여 중인 멤버입니다.");
                }

                ProjectMember member = ProjectMember.builder()
                                .project(project)
                                .user(targetUser)
                                .role(request.getRole() != null ? request.getRole() : ProjectRole.MEMBER)
                                .build();

                projectMemberRepository.save(member);
        }

        // 프로젝트 멤버 역할 변경
        @Transactional
        public void updateMemberRole(Long userId, Long projectId, Long targetUserId, ProjectRoleUpdateRequest request) {

                validateLeaderPermission(userId, projectId);

                User targetUser = userRepository.findById(targetUserId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                Project project = projectRepository.findById(projectId)
                        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "해당 프로젝트가 존재하지 않습니다."));

                ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 프로젝트에 속하지 않은 멤버입니다."));

                member.updateRole(request.getRole());
        }


        // 프로젝트 멤버 탈퇴
        @Transactional
        public void removeMember(Long userId, Long projectId, Long targetUserId) {

                // 본인이 탈퇴하는 경우
                if (userId.equals(targetUserId)) {
                        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                        projectMemberRepository.delete(member);
                        return;
                }

                // 타인을 추방하는 경우 리더인지 확인
                validateLeaderPermission(userId, projectId);

                ProjectMember targetMember = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                projectMemberRepository.delete(targetMember);
        }

        private void validateLeaderPermission(Long userId, Long projectId) {
                ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ACCESS_DENIED));

                if (member.getRole() != ProjectRole.ADMIN) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }
        }
}
