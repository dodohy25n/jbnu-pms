package jbnu.jbnupms.domain.user.service;

import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
<<<<<<< feat/common-response
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
=======
        User user = userRepository.findActiveById(Long.parseLong(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getIsDeleted()) {
<<<<<<< feat/user-refactor
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
=======
            throw new GlobalException(ErrorCode.USER_ALREADY_DELETED);
>>>>>>> feat/user-refactor
>>>>>>> main
        }

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),
                user.getPassword() != null ? user.getPassword() : "",
                new ArrayList<>()
        );
    }

    public User getUserById(Long userId) {
<<<<<<< feat/common-response
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
=======
        return userRepository.findActiveById(userId)
<<<<<<< feat/user-refactor
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
=======
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
>>>>>>> feat/user-refactor
>>>>>>> main
    }
}