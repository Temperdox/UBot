package com.cottonlesergal.ubot.repositories;

import com.cottonlesergal.ubot.entities.UserMessageActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserMessageActivityRepository extends JpaRepository<UserMessageActivity, String> {

    long countByUserId(String userId);

    long countByUserIdAndTimestampBetween(String userId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT DATE_FORMAT(timestamp, '%Y-%m-%d') as day, COUNT(*) as count " +
            "FROM UserMessageActivity " +
            "WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime " +
            "GROUP BY DATE_FORMAT(timestamp, '%Y-%m-%d') " +
            "ORDER BY day")
    List<Object[]> getMessageCountByDay(
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT HOUR(timestamp) as hour, COUNT(*) as count " +
            "FROM UserMessageActivity " +
            "WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime " +
            "GROUP BY HOUR(timestamp) " +
            "ORDER BY hour")
    List<Object[]> getMessageCountByHour(
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT channelId, COUNT(*) as count " +
            "FROM UserMessageActivity " +
            "WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime " +
            "GROUP BY channelId " +
            "ORDER BY count DESC " +
            "LIMIT :limit")
    List<Object[]> getTopChannels(
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit);
}