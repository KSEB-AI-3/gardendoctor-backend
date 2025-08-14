package com.project.farming.domain.user.repository;

import com.project.farming.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email); // AuthService.registerUser에서 사용
    List<User> findAllByOrderByUserIdAsc();
    List<User> findByNicknameContainingOrderByNicknameAsc(String keyword);
    List<User> findByEmailContainingOrderByEmailAsc(String keyword);

    // 이메일 찾기 기능을 위해 추가
    Optional<User> findByNickname(String nickname);

    @Query("SELECT u FROM User u WHERE u.fcmToken IS NOT NULL AND TRIM(u.fcmToken) <> ''")
    List<User> findUsersByFcmToken();
}
