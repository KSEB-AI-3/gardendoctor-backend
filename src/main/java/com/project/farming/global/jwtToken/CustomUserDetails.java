// src/main/java/com/project/farming/global/jwtToken/CustomUserDetails.java
package com.project.farming.global.jwtToken;

import com.project.farming.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User; // ⭐ OAuth2User 인터페이스 임포트 추가

import java.util.Collection;
import java.util.Collections;
import java.util.Map; // ⭐ Map 임포트 추가

// OAuth2User 인터페이스도 구현하도록 변경
// 이렇게 하면 일반 UserDetails와 OAuth2User 둘 다로 사용 가능합니다.
@Getter
public class CustomUserDetails implements UserDetails, OAuth2User { // ⭐ OAuth2User 인터페이스 추가
    private final User user;
    private Map<String, Object> attributes; // ⭐ OAuth2 속성을 저장할 필드 추가

    // 기존 UserDetails를 위한 생성자 (일반 로그인 사용자를 위해 유지)
    public CustomUserDetails(User user) {
        this.user = user;
    }

    // ⭐ OAuth2User를 위한 새로운 생성자 (CustomOAuth2UserService에서 호출할 생성자)
    public CustomUserDetails(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes; // OAuth2 속성 저장
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // User 엔티티의 role 필드를 기반으로 권한을 반환합니다.
        // 예: user.getRole().getKey()가 "ROLE_USER"라고 가정
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getKey()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // 로그인 시 사용할 사용자 식별자 (여기서는 이메일)
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // --- OAuth2User 인터페이스 구현 메서드 ---
    @Override
    public String getName() {
        // OAuth2User의 고유 식별자를 반환합니다.
        // 예를 들어, Google의 sub 또는 Kakao의 id를 사용할 수 있습니다.
        // 여기서는 User 엔티티의 oauthId를 반환하는 것이 가장 적합합니다.
        return user.getOauthId() != null ? user.getOauthId() : user.getEmail(); // 또는 다른 적절한 식별자를 반환하도록 수정 가능
    }

    @Override
    public Map<String, Object> getAttributes() {
        // OAuth2User의 원본 속성 맵을 반환합니다.
        return attributes;
    }
}