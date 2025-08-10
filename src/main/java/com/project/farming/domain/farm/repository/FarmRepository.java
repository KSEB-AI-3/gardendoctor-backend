package com.project.farming.domain.farm.repository;

import com.project.farming.domain.farm.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FarmRepository extends JpaRepository<Farm, Long> {
    boolean existsByGardenUniqueId(int gardenUniqueId);
    List<Farm> findAllByOrderByGardenUniqueIdAsc();
    List<Farm> findByFarmNameContainingOrderByGardenUniqueIdAsc(String keyword);

    @Query(value = """
        SELECT * FROM farm_info
        WHERE road_name_address LIKE :keyword OR lot_number_address LIKE :keyword
        ORDER BY garden_unique_id ASC
        """, nativeQuery = true)
    List<Farm> findByAddressContainingOrderByGardenUniqueIdAsc(@Param("keyword") String keyword);

    @Query(value = """
        SELECT * FROM farm_info
        WHERE farm_name LIKE :keyword
           OR road_name_address LIKE :keyword
           OR lot_number_address LIKE :keyword
        ORDER BY garden_unique_id ASC
        """, nativeQuery = true)
    List<Farm> findByFarmNameOrAddressContainingOrderByGardenUniqueIdAsc(@Param("keyword") String keyword);

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

    Optional<Farm> findByGardenUniqueId(int gardenUniqueId);

    @Query(value ="SELECT * FROM farm_info WHERE farm_name = :farmName LIMIT 1" , nativeQuery = true)
    Optional<Farm> getOtherFarm(@Param("farmName") String farmName);
}
