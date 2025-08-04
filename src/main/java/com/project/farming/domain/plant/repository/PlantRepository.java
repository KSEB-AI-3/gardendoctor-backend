package com.project.farming.domain.plant.repository;

import com.project.farming.domain.plant.entity.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant, Long> {
    boolean existsByPlantName(String plantName);
    List<Plant> findAllByOrderByPlantNameAsc();

    @Query(value = """
        SELECT * FROM plant_info
        WHERE plant_name LIKE :keyword OR plant_english_name LIKE :keyword
        ORDER BY plant_name ASC
        """, nativeQuery = true)
    List<Plant> findByPlantNameContainingOrderByPlantNameAsc(@Param("keyword") String keyword);

    List<Plant> findAllByOrderByPlantIdAsc();

    @Query(value = """
        SELECT * FROM plant_info
        WHERE plant_name LIKE :keyword OR plant_english_name LIKE :keyword
        ORDER BY plant_id ASC
        """, nativeQuery = true)
    List<Plant> findByPlantNameContainingOrderByPlantIdAsc(@Param("keyword") String keyword);

    @Query(value = """
        SELECT * FROM plant_info
        WHERE plant_name = :plantName OR plant_english_name = :plantName LIMIT 1
        """, nativeQuery = true)
    Optional<Plant> findByPlantName(@Param("plantName") String plantName);

    @Query(value ="SELECT * FROM plant_info WHERE plant_name = :plantName LIMIT 1" , nativeQuery = true)
    Optional<Plant> getOtherPlant(@Param("plantName") String plantName);
}
