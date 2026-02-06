package jbnu.jbnupms.security.oauth;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Google만 지원
        if (!"google".equals(registrationId)) {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        // Google 사용자 정보 추출
        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String profileImage = (String) attributes.get("picture");

        // 사용자 저장 또는 업데이트
        User user = saveOrUpdateUser(providerId, email, name, profileImage, registrationId);

        // OAuth2User 반환 (userId를 attributes에 추가)
        Map<String, Object> modifiedAttributes = Map.of(
                "sub", providerId,
                "email", email,
                "name", name,
                "picture", profileImage != null ? profileImage : "",
                "userId", user.getId()  // JWT 생성을 위해 추가
        );

        return new DefaultOAuth2User(
                Collections.emptyList(),
                modifiedAttributes,
                "sub"
        );
    }

    private User saveOrUpdateUser(String providerId, String email, String name,
                                  String profileImage, String provider) {
        // 1. provider + providerId로 기존 사용자 찾기
        return userRepository.findByProviderAndProviderId(provider.toUpperCase(), providerId)
                .map(existingUser -> {
                    // 기존 사용자 정보 업데이트 - 프로필 이미지 등 최신 정보로 동기화
                    existingUser.updateName(name);
                    if (profileImage != null) {
                        existingUser.updateProfileImage(profileImage);
                    }
                    log.info("OAuth2 user updated: {}", email);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // 2. 이메일로 일반 회원가입 사용자 확인
                    if (userRepository.existsByEmail(email)) {
                        throw new CustomException(ErrorCode.OAUTH2_EMAIL_ALREADY_REGISTERED);
                    }

                    // 3. 신규 사용자 생성
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .profileImage(profileImage)
                            .provider(provider.toUpperCase())
                            .providerId(providerId)
                            .password(null)
                            .build();

                    User savedUser = userRepository.save(newUser);
                    log.info("New OAuth2 user created: {}", email);
                    return savedUser;
                });
    }
}