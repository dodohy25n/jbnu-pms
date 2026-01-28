package jbnu.jbnupms.domain.user.service;

import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.common.exception.GlobalException;
import jbnu.jbnupms.domain.user.dto.UpdateUserRequest;
import jbnu.jbnupms.domain.user.dto.UserResponse;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.RefreshTokenRepository;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getMyInfo(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }

    public UserResponse getUserById(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long requestUserId, Long targetUserId, UpdateUserRequest request) {
        if (!requestUserId.equals(targetUserId)) {
            throw new GlobalException(ErrorCode.ACCESS_DENIED);
        }

        User user = findUserById(targetUserId);

        if (!user.getProvider().equals("EMAIL") && request.getPassword() != null) {
            throw new GlobalException(ErrorCode.ACCESS_DENIED);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            user.setPassword(encodedPassword);
        }

        userRepository.save(user);

        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long requestUserId, Long targetUserId) {
        if (!requestUserId.equals(targetUserId)) {
            throw new GlobalException(ErrorCode.ACCESS_DENIED);
        }

        User user = findUserById(targetUserId);

        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenRepository.deleteByUserId(targetUserId);
    }

    private User findUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new GlobalException(ErrorCode.USER_ALREADY_DELETED);
        }

        return user;
    }
}