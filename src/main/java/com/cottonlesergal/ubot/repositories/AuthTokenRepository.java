package com.cottonlesergal.ubot.repositories;

import com.cottonlesergal.ubot.entities.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    Optional<AuthToken> findByToken(String token);
    Optional<AuthToken> findByRefreshToken(String refreshToken);
    Optional<AuthToken> findByUserId(String userId);
    void deleteByExpiresAtBefore(LocalDateTime time);
}