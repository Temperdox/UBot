package com.cottonlesergal.ubot.repositories;

import com.cottonlesergal.ubot.entities.UserStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface UserStatusHistoryRepository extends JpaRepository<UserStatusHistory, String> {

    List<UserStatusHistory> findByUserIdOrderByStartTimeDesc(String userId);

    List<UserStatusHistory> findByUserIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeDesc(
            String userId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT status, SUM(duration) FROM UserStatusHistory " +
            "WHERE userId = :userId AND startTime >= :startTime AND " +
            "(endTime <= :endTime OR endTime IS NULL) " +
            "GROUP BY status")
    List<Object[]> getTotalDurationsByStatus(
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}