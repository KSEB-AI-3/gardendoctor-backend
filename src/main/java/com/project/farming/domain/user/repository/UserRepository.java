package com.project.farming.domain.user.repository;

import com.project.farming.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email); // AuthService.registerUser에서 사용
    List<User> findAllByOrderByNicknameAsc();
    List<User> findByNicknameContainingOrderByNicknameAsc(String keyword);
    List<User> findByEmailContainingOrderByEmailAsc(String keyword);
}
