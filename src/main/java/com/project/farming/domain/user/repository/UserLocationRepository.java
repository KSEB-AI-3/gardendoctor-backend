package com.project.farming.domain.user.repository;

import com.project.farming.domain.user.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {
    Optional<UserLocation> findByUser_UserId(Long userId);
}
