package com.homehn.backend.repository;

import com.homehn.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    List<UserEntity> findAllByOrderByCreatedAtDesc();
    List<UserEntity> findByRole(UserEntity.Role role);
}
