package com.homehn.backend.repository;

import com.homehn.backend.entity.ContractTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplateEntity, Long> {
    List<ContractTemplateEntity> findByLandlord_IdOrderByUpdatedAtDesc(Long landlordId);

    Optional<ContractTemplateEntity> findByIdAndLandlord_Id(Long id, Long landlordId);
}
