package jbnu.jbnupms.domain.project.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.project.dto.ProjectCreateRequest;
import jbnu.jbnupms.domain.project.dto.ProjectInviteRequest;
import jbnu.jbnupms.domain.project.dto.ProjectResponse;
import jbnu.jbnupms.domain.project.dto.ProjectRoleUpdateRequest;
import jbnu.jbnupms.domain.project.dto.ProjectUpdateRequest;
import jbnu.jbnupms.domain.project.dto.RecentProjectResponse;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;
import jbnu.jbnupms.domain.project.entity.ProjectStatus;
import jbnu.jbnupms.domain.project.repository.ProjectMemberRepository;
import jbnu.jbnupms.domain.project.repository.ProjectRepository;
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.repository.SpaceMemberRepository;
import jbnu.jbnupms.domain.space.repository.SpaceRepository;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND,
                                                "스페이스를 찾을 수 없습니다."));

                // Space 멤버인지 확인
                if (!spaceMemberRepository.existsBySpaceAndUser(space, user)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                // 프로젝트 생성
                Project project = Project.builder()
                                .space(space)
                                .name(request.getName())
                                .description(request.getDescription())
                                .dueDate(request.getDueDate())
                                .isPublic(request.getIsPublic())
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
        // - 멤버로 참여한 프로젝트 + 스페이스 내 public 프로젝트 모두 반환
        @Transactional
        public List<ProjectResponse> getProjects(Long userId, Long spaceId) {
                List<ProjectMember> myMemberships = projectMemberRepository.findByUserIdAndSpaceId(userId, spaceId);
                Set<Long> myProjectIds = myMemberships.stream()
                                .map(pm -> pm.getProject().getId())
                                .collect(Collectors.toCollection(HashSet::new));

                // 멤버 프로젝트 + 참여하지 않은 public 프로젝트 합산
                List<Project> allProjects = new ArrayList<>();
                myMemberships.forEach(pm -> allProjects.add(pm.getProject()));
                projectRepository.findPublicProjectsBySpaceId(spaceId).stream()
                                .filter(p -> !myProjectIds.contains(p.getId()))
                                .forEach(allProjects::add);

                List<Long> allProjectIds = allProjects.stream().map(Project::getId).collect(Collectors.toList());
                List<ProjectMember> allMembers = allProjectIds.isEmpty() ? List.of()
                                : projectMemberRepository.findByProjectIdIn(allProjectIds);

                return allProjects.stream()
                                .map(project -> {
                                        List<ProjectMember> members = allMembers.stream()
                                                        .filter(pm -> pm.getProject().getId().equals(project.getId()))
                                                        .collect(Collectors.toList());

                                        // 최근접근일시 업데이트 (내 멤버십이 있는 프로젝트만)
                                        if (myProjectIds.contains(project.getId())) {
                                                members.stream()
                                                                .filter(pm -> pm.getUser().getId().equals(userId))
                                                                .findFirst()
                                                                .ifPresent(ProjectMember::updateLastAccessedAt);
                                        }

                                        return ProjectResponse.from(project, members, userId);
                                })
                                .collect(Collectors.toList());
        }

        // 프로젝트 단건 조회
        // - 프로젝트 멤버 OR (public 프로젝트 + 스페이스 멤버)이면 조회 가능
        @Transactional
        public ProjectResponse getProject(Long userId, Long projectId) {
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                boolean isMember = projectMemberRepository.existsByProjectAndUser(project,
                                userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)));

                if (!isMember) {
                        // public 프로젝트이고 스페이스 멤버인 경우 읽기 허용
                        if (!project.getIsPublic() || !spaceMemberRepository.existsBySpaceIdAndUserId(project.getSpace().getId(), userId)) {
                                throw new CustomException(ErrorCode.ACCESS_DENIED);
                        }
                }

                List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

                // 최근접근일시 업데이트 (멤버인 경우만)
                if (isMember) {
                        members.stream()
                                        .filter(pm -> pm.getUser().getId().equals(userId))
                                        .findFirst()
                                        .ifPresent(ProjectMember::updateLastAccessedAt);
                }

                return ProjectResponse.from(project, members, userId);
        }

        // 최근 프로젝트 조회 (대시보드용)
        public List<RecentProjectResponse> getRecentProjects(Long userId, Long spaceId) {
                Pageable pageable = PageRequest.of(0, 3);
                return projectMemberRepository
                                .findTop3RecentProjects(userId, spaceId, ProjectStatus.IN_PROGRESS, pageable)
                                .stream()
                                .map(RecentProjectResponse::from)
                                .collect(Collectors.toList());
        }

        // 프로젝트 수정
        @Transactional
        public void updateProject(Long userId, Long projectId, ProjectUpdateRequest request) {
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                validateLeaderPermission(userId, projectId);

                project.update(request.getName(), request.getDescription(), request.getDueDate(),
                                request.getIsPublic(), request.getStatus());
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
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND,
                                                "해당 프로젝트를 찾을 수 없습니다."));

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

        // 프로젝트 멤버 목록만 따로 조회
        public List<ProjectResponse.MemberDto> getProjectMembers(Long userId, Long projectId) {
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                boolean isMember = projectMemberRepository.existsByProjectAndUser(project, user);
                if (!isMember) {
                        if (!project.getIsPublic() || !spaceMemberRepository.existsBySpaceIdAndUserId(project.getSpace().getId(), userId)) {
                                throw new CustomException(ErrorCode.ACCESS_DENIED);
                        }
                }

                List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

                return members.stream()
                                .map(ProjectResponse.MemberDto::new)
                                .collect(Collectors.toList());
        }

        // 프로젝트 멤버 역할 변경
        @Transactional
        public void updateMemberRole(Long userId, Long projectId, Long targetUserId, ProjectRoleUpdateRequest request) {

                validateLeaderPermission(userId, projectId);

                User targetUser = userRepository.findById(targetUserId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND,
                                                "해당 프로젝트가 존재하지 않습니다."));

                ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND,
                                                "해당 프로젝트에 속하지 않은 멤버입니다."));

                member.updateRole(request.getRole());
        }

        // 프로젝트 탈퇴 (본인)
        @Transactional
        public void leaveProject(Long userId, Long projectId) {
                ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND,
                                                "해당 프로젝트에 참여하고 있지 않습니다."));

                projectMemberRepository.delete(member);
        }

        // 프로젝트 멤버 추방 (관리자 권한 필요)
        @Transactional
        public void expelMember(Long userId, Long projectId, Long targetUserId) {
                // 관리자 권한 확인
                validateLeaderPermission(userId, projectId);

                // 추방 대상 멤버 조회
                ProjectMember targetMember = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 멤버를 찾을 수 없습니다."));

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
