package com.project.farming.domain.plant.repository;

import com.project.farming.domain.plant.entity.Plant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant, Long> {
    boolean existsByName(String name);
    List<Plant> findAllByOrderByName();
    Optional<Plant> findByName(String name);
}
