package jbnu.jbnupms.domain.space.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.space.dto.*;
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.entity.SpaceMember;
import jbnu.jbnupms.domain.space.entity.SpaceRole;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    @InjectMocks private SpaceService spaceService;
    @Mock private SpaceRepository spaceRepository;
    @Mock private SpaceMemberRepository spaceMemberRepository;
    @Mock private UserRepository userRepository;

    private User buildUser(String name) {
        return User.builder().email(name + "@test.com").password("pw").name(name).provider("EMAIL").build();
    }

    private Space buildSpace(User owner) {
        return Space.builder().name("테스트 스페이스").description("설명").owner(owner).build();
    }

    private SpaceMember buildAdminMember(Space space, User user) {
        return SpaceMember.builder().space(space).user(user).role(SpaceRole.ADMIN).build();
    }

    // SpaceCreateRequest 는 @NoArgsConstructor 만 있음 → 리플렉션으로 필드 직접 접근 불가
    // → 테스트용 헬퍼 메서드로 생성
    private SpaceCreateRequest spaceCreateRequest(String name, String description) {
        try {
            SpaceCreateRequest req = SpaceCreateRequest.class.getDeclaredConstructor().newInstance();
            setField(req, "name", name);
            setField(req, "description", description);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private SpaceUpdateRequest spaceUpdateRequest(String name, String description) {
        try {
            SpaceUpdateRequest req = SpaceUpdateRequest.class.getDeclaredConstructor().newInstance();
            setField(req, "name", name);
            setField(req, "description", description);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private SpaceInviteRequest spaceInviteRequest(String email, SpaceRole role) {
        try {
            SpaceInviteRequest req = SpaceInviteRequest.class.getDeclaredConstructor().newInstance();
            setField(req, "email", email);
            setField(req, "role", role);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private SpaceRoleUpdateRequest spaceRoleUpdateRequest(SpaceRole role) {
        try {
            SpaceRoleUpdateRequest req = SpaceRoleUpdateRequest.class.getDeclaredConstructor().newInstance();
            setField(req, "role", role);
            return req;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    @DisplayName("스페이스 생성 성공")
    void createSpace_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(spaceRepository.save(any())).willReturn(space);
        given(spaceMemberRepository.save(any())).willReturn(buildAdminMember(space, user));

        Long result = spaceService.createSpace(1L, spaceCreateRequest("테스트 스페이스", "설명"));

        assertThat(result).isEqualTo(space.getId());
        verify(spaceMemberRepository).save(any());
    }

    @Test
    @DisplayName("스페이스 생성 실패 - 사용자 없음")
    void createSpace_fail_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.createSpace(99L, spaceCreateRequest("스페이스", "")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("스페이스 목록 조회 성공")
    void getSpaces_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        given(spaceMemberRepository.findByUserId(1L)).willReturn(List.of(buildAdminMember(space, user)));

        List<SpaceResponse> result = spaceService.getSpaces(1L);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("스페이스 단건 조회 성공")
    void getSpace_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);

        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(spaceMemberRepository.existsBySpaceAndUser(space, user)).willReturn(true);
        given(spaceMemberRepository.findBySpaceId(1L)).willReturn(List.of());

        SpaceDetailResponse result = spaceService.getSpace(1L, 1L);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("스페이스 단건 조회 실패 - 멤버 아님")
    void getSpace_fail_notMember() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);

        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(spaceMemberRepository.existsBySpaceAndUser(space, user)).willReturn(false);

        assertThatThrownBy(() -> spaceService.getSpace(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("스페이스 수정 성공")
    void updateSpace_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        SpaceMember admin = buildAdminMember(space, user);

        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(spaceMemberRepository.findByUserIdAndSpaceId(1L, 1L)).willReturn(Optional.of(admin));

        spaceService.updateSpace(1L, 1L, spaceUpdateRequest("수정된 이름", "수정된 설명"));

        assertThat(space.getName()).isEqualTo("수정된 이름");
    }

    @Test
    @DisplayName("스페이스 수정 실패 - ADMIN 아님")
    void updateSpace_fail_notAdmin() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        SpaceMember member = SpaceMember.builder().space(space).user(user).role(SpaceRole.MEMBER).build();

        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(spaceMemberRepository.findByUserIdAndSpaceId(1L, 1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> spaceService.updateSpace(1L, 1L, spaceUpdateRequest("수정", "")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("스페이스 삭제 성공")
    void deleteSpace_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);

        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(spaceMemberRepository.findByUserIdAndSpaceId(1L, 1L)).willReturn(Optional.of(buildAdminMember(space, user)));

        spaceService.deleteSpace(1L, 1L);

        verify(spaceRepository).delete(space);
    }

    @Test
    @DisplayName("스페이스 멤버 초대 성공")
    void inviteMember_success() {
        User admin = buildUser("관리자");
        User target = buildUser("초대대상");
        Space space = buildSpace(admin);

        given(spaceMemberRepository.findByUserIdAndSpaceId(1L, 1L)).willReturn(Optional.of(buildAdminMember(space, admin)));
        given(userRepository.findByEmail("초대대상@test.com")).willReturn(Optional.of(target));
        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(spaceMemberRepository.existsBySpaceAndUser(space, target)).willReturn(false);

        spaceService.inviteMember(1L, 1L, spaceInviteRequest("초대대상@test.com", SpaceRole.MEMBER));

        verify(spaceMemberRepository).save(any());
    }

    @Test
    @DisplayName("스페이스 멤버 초대 실패 - 이미 멤버")
    void inviteMember_fail_alreadyMember() {
        User admin = buildUser("관리자");
        User target = buildUser("초대대상");
        Space space = buildSpace(admin);

        given(spaceMemberRepository.findByUserIdAndSpaceId(1L, 1L)).willReturn(Optional.of(buildAdminMember(space, admin)));
        given(userRepository.findByEmail("초대대상@test.com")).willReturn(Optional.of(target));
        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
        given(spaceMemberRepository.existsBySpaceAndUser(space, target)).willReturn(true);

        assertThatThrownBy(() -> spaceService.inviteMember(1L, 1L, spaceInviteRequest("초대대상@test.com", null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    @DisplayName("스페이스 탈퇴 성공")
    void leaveSpace_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);
        SpaceMember member = buildAdminMember(space, user);

        given(spaceMemberRepository.findByUserIdAndSpaceId(1L, 1L)).willReturn(Optional.of(member));

        spaceService.leaveSpace(1L, 1L);

        verify(spaceMemberRepository).delete(member);
    }

    @Test
    @DisplayName("스페이스 멤버 추방 성공")
    void expelMember_success() {
        User admin = buildUser("관리자");
        User target = buildUser("대상");
        Space space = buildSpace(admin);
        SpaceMember targetMember = SpaceMember.builder().space(space).user(target).role(SpaceRole.MEMBER).build();

        given(spaceMemberRepository.findByUserIdAndSpaceId(1L, 1L)).willReturn(Optional.of(buildAdminMember(space, admin)));
        given(spaceMemberRepository.findByUserIdAndSpaceId(2L, 1L)).willReturn(Optional.of(targetMember));

        spaceService.expelMember(1L, 1L, 2L);

        verify(spaceMemberRepository).delete(targetMember);
    }

    @Test
    @DisplayName("스페이스 멤버 역할 변경 성공")
    void updateMemberRole_success() {
        User admin = buildUser("관리자");
        User target = buildUser("대상");
        Space space = buildSpace(admin);
        SpaceMember adminMember = buildAdminMember(space, admin);
        SpaceMember targetMember = SpaceMember.builder().space(space).user(target).role(SpaceRole.MEMBER).build();

        // eq() matcher로 명확히 구분 - (1L,1L)은 adminMember, (2L,1L)은 targetMember
        given(spaceMemberRepository.findByUserIdAndSpaceId(eq(1L), eq(1L))).willReturn(Optional.of(adminMember));
        given(spaceMemberRepository.findByUserIdAndSpaceId(eq(2L), eq(1L))).willReturn(Optional.of(targetMember));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(spaceRepository.findById(1L)).willReturn(Optional.of(space));

        spaceService.updateMemberRole(1L, 1L, 2L, spaceRoleUpdateRequest(SpaceRole.ADMIN));

        assertThat(targetMember.getRole()).isEqualTo(SpaceRole.ADMIN);
    }
}