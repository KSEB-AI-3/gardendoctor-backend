package com.project.farming.domain.plant.repository;

import com.project.farming.domain.plant.entity.UserPlant;
import com.project.farming.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPlantRepository extends JpaRepository<UserPlant, Long>  {
    boolean existsByUserAndNickname(User user, String nickname);
    List<UserPlant> findByUserOrderByNicknameAsc(User user);
    Optional<UserPlant> findByUserAndUserPlantId(User user, Long userPlantId);
}
