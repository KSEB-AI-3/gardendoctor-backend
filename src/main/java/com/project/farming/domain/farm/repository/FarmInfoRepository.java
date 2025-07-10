package com.project.farming.domain.farm.repository;

import com.project.farming.domain.farm.entity.FarmInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FarmInfoRepository extends JpaRepository<FarmInfo, Long> {
    boolean existsByGardenUniqueId(Integer gardenUniqueId);

    @Query(value ="SELECT * FROM farm_info WHERE name = :name LIMIT 1" , nativeQuery = true)
    Optional<FarmInfo> getDummyFarm(@Param("name") String name);

    List<FarmInfo> findAllByOrderByGardenUniqueIdAsc();
    Optional<FarmInfo> findByGardenUniqueId(Integer gardenUniqueId);
}
