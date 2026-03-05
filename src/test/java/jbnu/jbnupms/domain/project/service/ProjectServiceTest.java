package jbnu.jbnupms.domain.project.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.project.dto.*;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @InjectMocks private ProjectService projectService;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private SpaceRepository spaceRepository;
    @Mock private SpaceMemberRepository spaceMemberRepository;
    @Mock private UserRepository userRepository;

    private User buildUser(String name) {
        return User.builder().email(name + "@test.com").password("pw").name(name).provider("EMAIL").build();
    }
    private Space buildSpace(User owner) {
        return Space.builder().name("스페이스").description("설명").owner(owner).build();
    }
    private Project buildProject(Space space) {
        return Project.builder().space(space).name("프로젝트").description("설명").build();
    }
    private ProjectMember buildAdminMember(Project project, User user) {
        return ProjectMember.builder().project(project).user(user).role(ProjectRole.ADMIN).build();
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private ProjectCreateRequest projectCreateRequest(Long spaceId, String name, String description) {
        try {
            ProjectCreateRequest req = new ProjectCreateRequest();
            setField(req, "spaceId", spaceId);
            setField(req, "name", name);
            setField(req, "description", description);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private ProjectUpdateRequest projectUpdateRequest(String name, String description) {
        try {
            ProjectUpdateRequest req = new ProjectUpdateRequest();
            setField(req, "name", name);
            setField(req, "description", description);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private ProjectInviteRequest projectInviteRequest(String email, ProjectRole role) {
        try {
            ProjectInviteRequest req = new ProjectInviteRequest();
            setField(req, "email", email);
            setField(req, "role", role);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    @DisplayName("프로젝트 생성 성공")
    void createProject_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(spaceMemberRepository.existsBySpaceAndUser(space, user)).willReturn(true);
        given(projectRepository.save(any())).willReturn(project);
        given(projectMemberRepository.save(any())).willReturn(buildAdminMember(project, user));

        Long result = projectService.createProject(1L, projectCreateRequest(1L, "프로젝트", "설명"));

        assertThat(result).isEqualTo(project.getId());
        verify(projectMemberRepository).save(any());
    }

    @Test
    @DisplayName("프로젝트 생성 실패 - 스페이스 멤버 아님")
    void createProject_fail_notSpaceMember() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(spaceMemberRepository.existsBySpaceAndUser(space, user)).willReturn(false);

        assertThatThrownBy(() -> projectService.createProject(1L, projectCreateRequest(1L, "프로젝트", "")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("프로젝트 단건 조회 성공")
    void getProject_success() {
        User user = buildUser("홍길동");
        // user.id가 null이면 ProjectService 127번줄 pm.getUser().getId().equals(userId) NPE 발생
        // 리플렉션으로 id=1L 주입
        setField(user, "id", 1L);
        Space space = buildSpace(user);
        Project project = buildProject(space);

        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectAndUser(project, user)).willReturn(true);
        given(projectMemberRepository.findByProjectId(1L)).willReturn(List.of(buildAdminMember(project, user)));

        ProjectResponse result = projectService.getProject(1L, 1L);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("프로젝트 단건 조회 실패 - 멤버 아님")
    void getProject_fail_notMember() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);

        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectMemberRepository.existsByProjectAndUser(project, user)).willReturn(false);

        assertThatThrownBy(() -> projectService.getProject(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("프로젝트 수정 성공")
    void updateProject_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);

        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).willReturn(Optional.of(buildAdminMember(project, user)));

        projectService.updateProject(1L, 1L, projectUpdateRequest("수정된 프로젝트", "수정된 설명"));

        assertThat(project.getName()).isEqualTo("수정된 프로젝트");
    }

    @Test
    @DisplayName("프로젝트 수정 실패 - ADMIN 아님")
    void updateProject_fail_notAdmin() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        ProjectMember member = ProjectMember.builder().project(project).user(user).role(ProjectRole.MEMBER).build();

        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> projectService.updateProject(1L, 1L, projectUpdateRequest("수정", "")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("프로젝트 삭제 성공")
    void deleteProject_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);

        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).willReturn(Optional.of(buildAdminMember(project, user)));

        projectService.deleteProject(1L, 1L);

        verify(projectRepository).delete(project);
    }

    @Test
    @DisplayName("프로젝트 멤버 초대 성공")
    void inviteMember_success() {
        User admin = buildUser("관리자");
        User target = buildUser("초대대상");
        Space space = buildSpace(admin);
        Project project = buildProject(space);

        given(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).willReturn(Optional.of(buildAdminMember(project, admin)));
        given(userRepository.findByEmail("초대대상@test.com")).willReturn(Optional.of(target));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(projectMemberRepository.existsByProjectIdAndUserId(1L, target.getId())).willReturn(false);

        projectService.inviteMember(1L, 1L, projectInviteRequest("초대대상@test.com", ProjectRole.MEMBER));

        verify(projectMemberRepository).save(any());
    }

    @Test
    @DisplayName("프로젝트 멤버 초대 실패 - 이미 멤버")
    void inviteMember_fail_alreadyMember() {
        User admin = buildUser("관리자");
        User target = buildUser("초대대상");
        Space space = buildSpace(admin);
        Project project = buildProject(space);

        given(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).willReturn(Optional.of(buildAdminMember(project, admin)));
        given(userRepository.findByEmail("초대대상@test.com")).willReturn(Optional.of(target));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(projectMemberRepository.existsByProjectIdAndUserId(1L, target.getId())).willReturn(true);

        assertThatThrownBy(() -> projectService.inviteMember(1L, 1L, projectInviteRequest("초대대상@test.com", null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    @DisplayName("프로젝트 탈퇴 성공")
    void leaveProject_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        Project project = buildProject(space);
        ProjectMember member = buildAdminMember(project, user);

        given(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).willReturn(Optional.of(member));

        projectService.leaveProject(1L, 1L);

        verify(projectMemberRepository).delete(member);
    }

    @Test
    @DisplayName("프로젝트 멤버 추방 성공")
    void expelMember_success() {
        User admin = buildUser("관리자");
        User target = buildUser("대상");
        Space space = buildSpace(admin);
        Project project = buildProject(space);
        ProjectMember targetMember = ProjectMember.builder().project(project).user(target).role(ProjectRole.MEMBER).build();

        given(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).willReturn(Optional.of(buildAdminMember(project, admin)));
        given(projectMemberRepository.findByProjectIdAndUserId(1L, 2L)).willReturn(Optional.of(targetMember));

        projectService.expelMember(1L, 1L, 2L);

        verify(projectMemberRepository).delete(targetMember);
    }
}