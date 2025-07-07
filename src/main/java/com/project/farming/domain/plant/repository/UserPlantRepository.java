package com.project.farming.domain.plant.repository;

import com.project.farming.domain.plant.entity.UserPlant;
import com.project.farming.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPlantRepository extends JpaRepository<UserPlant, Long>  {
    boolean existsByUserAndNickname(User user, String nickname);
    List<UserPlant> findAllByEmailOrderByNickname(String email);
    Optional<UserPlant> findByEmailAndNickname(String email, String nickname);
}
