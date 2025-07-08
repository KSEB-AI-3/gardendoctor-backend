package com.project.farming.domain.user.repository;

import com.project.farming.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // OAuth Provider와 해당 Provider의 고유 ID로 사용자 조회
    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    // ⭐ 이메일로 사용자 조회 (일반 로그인 및 이메일 중복 검사용)
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}