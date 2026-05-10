package com.homehn.backend.repository;

import com.homehn.backend.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    Optional<PasswordResetTokenEntity> findByToken(String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
