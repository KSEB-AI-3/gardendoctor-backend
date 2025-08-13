package com.project.farming.domain.userplant.repository;

import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.plant.entity.Plant;
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
    @Query("UPDATE UserPlant up SET up.farm = :otherFarm WHERE up.farm = :oldFarm")
    int reassignFarm(
            @Param("otherFarm") Farm otherFarm, @Param("oldFarm") Farm oldFarm);

    @Modifying
    @Query("UPDATE UserPlant up SET up.plant = :otherPlant WHERE up.plant = :oldPlant")
    int reassignPlant(
            @Param("otherPlant") Plant otherPlant, @Param("oldPlant") Plant oldPlant);

    @Query("""
        SELECT up FROM UserPlant up
        WHERE up.isNotificationEnabled = true
          AND up.lastWateredDate IS NOT NULL
          AND FUNCTION('DATEDIFF', CURRENT_DATE, up.lastWateredDate) >= up.waterIntervalDays
        """)
    List<UserPlant> findUserPlantsNeedWateringToday();

    @Query("""
        SELECT up FROM UserPlant up
        WHERE up.isNotificationEnabled = true
          AND up.lastPrunedDate IS NOT NULL
          AND FUNCTION('DATEDIFF', CURRENT_DATE, up.lastPrunedDate) >= up.pruneIntervalDays
        """)
    List<UserPlant> findUserPlantsNeedPruningToday();

    @Query("""
        SELECT up FROM UserPlant up
        WHERE up.isNotificationEnabled = true
          AND up.lastFertilizedDate IS NOT NULL
          AND FUNCTION('DATEDIFF', CURRENT_DATE, up.lastFertilizedDate) >= up.fertilizeIntervalDays
        """)
    List<UserPlant> findUserPlantsNeedFertilizingToday();

    @Query("""
        SELECT up FROM UserPlant up
        WHERE up.isNotificationEnabled = true
          AND up.watered = false
          AND up.lastWateredDate IS NOT NULL
          AND FUNCTION('DATEDIFF', CURRENT_DATE, up.lastWateredDate) >= up.waterIntervalDays
        """)
    List<UserPlant> findUserPlantsIncompleteWateringToday();

    @Query("""
        SELECT up FROM UserPlant up
        WHERE up.isNotificationEnabled = true
          AND up.pruned = false
          AND up.lastPrunedDate IS NOT NULL
          AND FUNCTION('DATEDIFF', CURRENT_DATE, up.lastPrunedDate) >= up.pruneIntervalDays
        """)
    List<UserPlant> findUserPlantsIncompletePruningToday();

    @Query("""
        SELECT up FROM UserPlant up
        WHERE up.isNotificationEnabled = true
          AND up.fertilized = false
          AND up.lastFertilizedDate IS NOT NULL
          AND FUNCTION('DATEDIFF', CURRENT_DATE, up.lastFertilizedDate) >= up.fertilizeIntervalDays
        """)
    List<UserPlant> findUserPlantsIncompleteFertilizingToday();
}
