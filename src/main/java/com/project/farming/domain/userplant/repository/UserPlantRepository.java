package com.project.farming.domain.userplant.repository;

import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.userplant.entity.UserPlant;
import com.project.farming.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPlantRepository extends JpaRepository<UserPlant, Long>  {
    boolean existsByUserAndPlantNickname(User user, String plantNickname);
    List<UserPlant> findByUserOrderByPlantNicknameAsc(User user);

    @Query(value = """
        SELECT * FROM user_plants
        WHERE user_id = :userId
          AND (plant_name LIKE :keyword OR plant_nickname LIKE :keyword)
        ORDER BY plant_nickname ASC
        """, nativeQuery = true)
    List<UserPlant> findByUserAndPlantContainingOrderByPlantNicknameAsc(
            @Param("userId") Long userId, @Param("keyword") String keyword);

    Optional<UserPlant> findByUserAndUserPlantId(User user, Long userPlantId);

    @Query("SELECT up FROM UserPlant up JOIN FETCH up.user JOIN FETCH up.plant")
    List<UserPlant> findAllWithUserAndPlant();

    @Modifying
    @Query("""
            UPDATE UserPlant up
            SET up.farm = :otherFarm, up.plantingPlace = :place
            WHERE up.farm = :oldFarm
            """)
    int reassignFarm(
            @Param("otherFarm") Farm otherFarm,
            @Param("place") String place,
            @Param("oldFarm") Farm oldFarm);
}
