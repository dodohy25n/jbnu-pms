package jbnu.jbnupms.domain.space.service;

import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.entity.SpaceMember;
import jbnu.jbnupms.domain.space.repository.SpaceMemberRepository;
import jbnu.jbnupms.domain.space.repository.SpaceRepository;
import jbnu.jbnupms.domain.space.dto.CreateSpaceRequest;
import jbnu.jbnupms.domain.space.dto.SpaceDetailResponse;
import jbnu.jbnupms.domain.space.dto.SpaceResponse;
import jbnu.jbnupms.domain.space.dto.UpdateSpaceRequest;
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

        @Transactional
        public Long createSpace(CreateSpaceRequest request) {
                User owner = userRepository.findById(request.getOwnerId())
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));

                Space space = Space.builder()
                                .name(request.getName())
                                .description(request.getDescription())
                                .owner(owner)
                                .build();

                Space savedSpace = spaceRepository.save(space);

                SpaceMember spaceMember = SpaceMember.builder()
                                .space(savedSpace)
                                .user(owner)
                                .role(SpaceMember.SpaceRole.ADMIN) // 생성자는 ADMIN으로
                                .build();
                spaceMemberRepository.save(spaceMember);

                return savedSpace.getId();
        }

        @Transactional
        public void updateSpace(Long spaceId, UpdateSpaceRequest request) {
                Space space = spaceRepository.findById(spaceId)
                                .orElseThrow(() -> new IllegalArgumentException("Space not found"));

                // todo: 로그인한 유저 ADMIN인지 권한 확인

                space.update(request.getName(), request.getDescription());
        }

        @Transactional
        public void deleteSpace(Long spaceId) {
                Space space = spaceRepository.findById(spaceId)
                                .orElseThrow(() -> new IllegalArgumentException("Space not found"));

                // todo: 로그인한 유저 ADMIN인지 권한 확인

                spaceRepository.delete(space);
        }

        public SpaceDetailResponse getSpace(Long spaceId) {
                Space space = spaceRepository.findById(spaceId)
                                .orElseThrow(() -> new IllegalArgumentException("Space not found"));

                List<SpaceMember> members = spaceMemberRepository.findBySpaceId(spaceId);

                return new SpaceDetailResponse(space, members);
        }

        public List<SpaceResponse> getSpaces(Long userId) {

                List<SpaceMember> memberships = spaceMemberRepository.findByUserId(userId);

                return memberships.stream()
                                .map(SpaceMember::getSpace)
                                .map(SpaceResponse::new)
                                .collect(Collectors.toList());
        }
}
