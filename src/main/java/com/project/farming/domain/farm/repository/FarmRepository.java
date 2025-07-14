package com.project.farming.domain.farm.repository;

import com.project.farming.domain.farm.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FarmRepository extends JpaRepository<Farm, Long> {
    boolean existsByGardenUniqueId(Integer gardenUniqueId);

    @Query(value ="SELECT * FROM farm_info WHERE name = :name LIMIT 1" , nativeQuery = true)
    Optional<Farm> getDummyFarm(@Param("name") String name);

    List<Farm> findAllByOrderByGardenUniqueIdAsc();

    @Query(value = """
        SELECT * FROM farm_info f
        WHERE ST_Distance_Sphere(
            POINT(:longitude, :latitude),
            POINT(f.longitude, f.latitude)
        ) <= :radius
        """, nativeQuery = true)
    List<Farm> findFarmsWithinRadius(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radius") Double radius);

    Optional<Farm> findByGardenUniqueId(Integer gardenUniqueId);
}
