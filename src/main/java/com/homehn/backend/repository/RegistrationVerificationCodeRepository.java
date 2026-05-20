package com.homehn.backend.repository;

import com.homehn.backend.entity.RegistrationVerificationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RegistrationVerificationCodeRepository extends JpaRepository<RegistrationVerificationCodeEntity, Long> {
    Optional<RegistrationVerificationCodeEntity> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @Transactional
    @Modifying
    @Query("DELETE FROM RegistrationVerificationCodeEntity c WHERE lower(c.email) = lower(:email)")
    void deleteByEmailIgnoreCase(@Param("email") String email);
}
