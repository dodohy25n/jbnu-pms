package jbnu.jbnupms.domain.space.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.space.dto.SpaceCreateRequest;
import jbnu.jbnupms.domain.space.dto.SpaceDetailResponse;
import jbnu.jbnupms.domain.space.dto.SpaceInviteRequest;
import jbnu.jbnupms.domain.space.dto.SpaceResponse;
import jbnu.jbnupms.domain.space.dto.SpaceRoleUpdateRequest;
import jbnu.jbnupms.domain.space.dto.SpaceUpdateRequest;
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.entity.SpaceMember;
import jbnu.jbnupms.domain.space.entity.SpaceRole;
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
public class SpaceService {

        private final SpaceRepository spaceRepository;
        private final SpaceMemberRepository spaceMemberRepository;
        private final UserRepository userRepository;

        // 스페이스 생성
        @Transactional
        public Long createSpace(Long userId, SpaceCreateRequest request) {
                User owner = userRepository.findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                Space space = Space.builder()
                        .name(request.getName())
                        .description(request.getDescription())
                        .owner(owner)
                                .build();

                Space savedSpace = spaceRepository.save(space);

                // 생성자는 ADMIN으로 추가
                SpaceMember spaceMember = SpaceMember.builder()
                        .space(savedSpace)
                        .user(owner)
                        .role(SpaceRole.ADMIN)
                        .build();
                spaceMemberRepository.save(spaceMember);

                return savedSpace.getId();
        }

        // 사용자가 속한 스페이스 목록 조회
        public List<SpaceResponse> getSpaces(Long userId) {

                List<SpaceMember> memberships = spaceMemberRepository.findByUserId(userId);

                return memberships.stream()
                        .map(SpaceMember::getSpace)
                        .map(SpaceResponse::from)
                        .collect(Collectors.toList());
        }

        // 스페이스 단건 조회
        public SpaceDetailResponse getSpace(Long userId, Long spaceId) {
                Space space = spaceRepository.findById(spaceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                User user = userRepository.findActiveById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                // 멤버인지 확인
                if (!spaceMemberRepository.existsBySpaceAndUser(space, user)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                List<SpaceMember> members = spaceMemberRepository.findBySpaceId(spaceId);

                return SpaceDetailResponse.from(space, members);
        }

        // 스페이스 수정
        @Transactional
        public void updateSpace(Long userId, Long spaceId, SpaceUpdateRequest request) {
                Space space = spaceRepository.findById(spaceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                validateAdminPermission(userId, spaceId);

                space.update(request.getName(), request.getDescription());
        }

        // 스페이스 삭제
        @Transactional
        public void deleteSpace(Long userId, Long spaceId) {
                Space space = spaceRepository.findById(spaceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

                validateAdminPermission(userId, spaceId);

                spaceRepository.delete(space);
        }

        // 스페이스 멤버 초대
        @Transactional
        public void inviteMember(Long userId, Long spaceId, SpaceInviteRequest request) {

                // 리더인지 확인
                validateAdminPermission(userId, spaceId);

                User targetUser = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                Space space = spaceRepository.findById(spaceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "해당 스페이스를 찾을 수 없습니다."));

                // 이미 멤버인지 확인
                if (spaceMemberRepository.existsBySpaceAndUser(space, targetUser)) {
                        throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 참여 중인 멤버입니다.");
                }

                SpaceMember member = SpaceMember.builder()
                        .space(space)
                        .user(targetUser)
                        .role(request.getRole() != null ? request.getRole() : SpaceRole.MEMBER)
                        .build();

                spaceMemberRepository.save(member);
        }

        // 스페이스 멤버 역할 변경
        @Transactional
        public void updateMemberRole(Long userId, Long spaceId, Long targetUserId, SpaceRoleUpdateRequest request) {

                validateAdminPermission(userId, spaceId);

                User targetUser = userRepository.findActiveById(targetUserId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                Space space = spaceRepository.findById(spaceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "해당 스페이스가 존재하지 않습니다."));

                SpaceMember member = spaceMemberRepository.findByUserIdAndSpaceId(userId, spaceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 스페이스에 속하지 않은 멤버입니다."));

                member.updateRole(request.getRole());
        }

        // 스페이스 탈퇴 (본인)
        @Transactional
        public void leaveSpace(Long userId, Long spaceId) {
                SpaceMember member = spaceMemberRepository.findByUserIdAndSpaceId(userId, spaceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "해당 스페이스에 참여하고 있지 않습니다."));

                spaceMemberRepository.delete(member);
        }

        // 스페이스 멤버 추방 (관리자 권한 필요)
        @Transactional
        public void expelMember(Long userId, Long spaceId, Long targetUserId) {
                // 관리자 권한 확인
                validateAdminPermission(userId, spaceId);

                // 추방 대상 멤버 조회
                SpaceMember targetMember = spaceMemberRepository.findByUserIdAndSpaceId(targetUserId, spaceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 멤버를 찾을 수 없습니다."));

                spaceMemberRepository.delete(targetMember);
        }

        private void validateAdminPermission(Long userId, Long spaceId) {

                SpaceMember member = spaceMemberRepository.findByUserIdAndSpaceId(userId, spaceId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ACCESS_DENIED));

                if (member.getRole() != SpaceRole.ADMIN) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }
        }
}