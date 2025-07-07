package com.project.farming.domain.plant.repository;

import com.project.farming.domain.plant.entity.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant, Long> {
    boolean existsByName(String name);

    @Query(value ="SELECT * FROM plant_info WHERE name = :name LIMIT 1" , nativeQuery = true)
    Optional<Plant> getDummyPlant(@Param("name") String name);

    List<Plant> findAllByOrderByNameAsc();
    Optional<Plant> findByName(String name);
}
