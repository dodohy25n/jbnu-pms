package jbnu.jbnupms.security.oauth;

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

        if (!"google".equals(registrationId)) {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String profileImage = (String) attributes.get("picture");

        User user = saveOrUpdateUser(providerId, email, name, profileImage, registrationId);

        Map<String, Object> modifiedAttributes = Map.of(
                "sub", providerId,
                "email", email,
                "name", name,
                "picture", profileImage != null ? profileImage : "",
                "userId", user.getId()
        );

        return new DefaultOAuth2User(
                Collections.emptyList(),
                modifiedAttributes,
                "sub"
        );
    }

    private User saveOrUpdateUser(String providerId, String email, String name,
                                  String profileImage, String provider) {
        String providerUpper = provider.toUpperCase();

        // 1. provider + providerId로 찾기
        return userRepository.findByProviderAndProviderId(providerUpper, providerId)
                .map(existingUser -> {
                    existingUser.updateName(name);
                    if (profileImage != null) {
                        existingUser.updateProfileImage(profileImage);
                    }
                    log.info("OAuth2 user updated: {}", email);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // 2. 이메일로 찾기
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {
                                // 기존 일반 회원: provider, providerId 업데이트
                                String currentProvider = existingUser.getProvider();
                                if (!currentProvider.contains(providerUpper)) {
                                    existingUser.setProvider(currentProvider + "," + providerUpper);
                                }
                                existingUser.setProviderId(providerId);
                                if (profileImage != null) {
                                    existingUser.updateProfileImage(profileImage);
                                }
                                User updated = userRepository.save(existingUser);
                                log.info("Added {} to existing user: {}", providerUpper, email);
                                return updated;
                            })
                            .orElseGet(() -> {
                                // 3. 신규 생성
                                User newUser = User.builder()
                                        .email(email)
                                        .name(name)
                                        .profileImage(profileImage)
                                        .provider(providerUpper)
                                        .providerId(providerId)
                                        .password(null)
                                        .build();

                                User saved = userRepository.save(newUser);
                                log.info("New OAuth2 user created: {}", email);
                                return saved;
                            });
                });
    }
}