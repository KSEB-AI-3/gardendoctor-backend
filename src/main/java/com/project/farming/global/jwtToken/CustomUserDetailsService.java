package com.project.farming.global.jwtToken;

import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userIdStr) throws UsernameNotFoundException {
        try {
            Long userId = Long.parseLong(userIdStr); // JWT sub로 전달된 userId 파싱

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("해당하는 사용자를 찾을 수 없습니다: " + userId));

            return new CustomUserDetails(user);

        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("잘못된 userId 형식: " + userIdStr);
        }
    }
}
