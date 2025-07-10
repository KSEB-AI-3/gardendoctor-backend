package com.project.farming.domain.farm.repository;

import com.project.farming.domain.farm.entity.FarmInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FarmInfoRepository extends JpaRepository<FarmInfo, Long> {
    boolean existsByGardenUniqueId(Integer gardenUniqueId);
    List<FarmInfo> findAllByOrderByGardenUniqueIdAsc();
}
