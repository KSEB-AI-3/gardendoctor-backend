package com.project.farming.domain.farm.repository;

import com.project.farming.domain.farm.entity.FarmInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmInfoRepository extends JpaRepository<FarmInfo, Long> {
    // 필요 시 확장
}
