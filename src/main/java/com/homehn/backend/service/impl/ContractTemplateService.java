package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.SaveContractTemplateRequest;
import com.homehn.backend.dto.response.ContractTemplateResponse;
import com.homehn.backend.entity.ContractTemplateEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.ContractTemplateRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.util.ContractTermsFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractTemplateService {

    private final ContractTemplateRepository templateRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<ContractTemplateResponse> getMyTemplates(UserPrincipal principal) {
        ensureLandlordOrAdmin(principal);
        return templateRepo.findByLandlord_IdOrderByUpdatedAtDesc(principal.getId()).stream()
                .map(ContractTemplateResponse::from)
                .toList();
    }

    public ContractTemplateResponse create(SaveContractTemplateRequest req, UserPrincipal principal) {
        ensureLandlordOrAdmin(principal);
        UserEntity landlord = userRepo.findById(principal.getId())
                .orElseThrow(() -> new AppException("Không tìm thấy chủ trọ", 404));

        ContractTemplateEntity template = templateRepo.save(ContractTemplateEntity.builder()
                .landlord(landlord)
                .name(req.getName().trim())
                .content(ContractTermsFormatter.format(
                        req.getMoveInRules(),
                        req.getDefaultMonthlyRent(),
                        req.getDefaultDepositAmount(),
                        req.getDefaultElectricPrice(),
                        req.getDefaultWaterPrice(),
                        req.getDefaultOtherFees(),
                        req.getServiceNotes(),
                        req.getAdditionalTerms()
                ))
                .defaultMonthlyRent(req.getDefaultMonthlyRent())
                .defaultDepositAmount(req.getDefaultDepositAmount())
                .defaultElectricPrice(req.getDefaultElectricPrice())
                .defaultWaterPrice(req.getDefaultWaterPrice())
                .defaultOtherFees(req.getDefaultOtherFees())
                .moveInRules(req.getMoveInRules().trim())
                .serviceNotes(req.getServiceNotes().trim())
                .additionalTerms(blankToNull(req.getAdditionalTerms()))
                .build());
        return ContractTemplateResponse.from(template);
    }

    public ContractTemplateResponse update(Long templateId, SaveContractTemplateRequest req, UserPrincipal principal) {
        ensureLandlordOrAdmin(principal);
        ContractTemplateEntity template = templateRepo.findByIdAndLandlord_Id(templateId, principal.getId())
                .orElseThrow(() -> new AppException("Không tìm thấy mẫu hợp đồng", 404));

        template.setName(req.getName().trim());
        template.setDefaultMonthlyRent(req.getDefaultMonthlyRent());
        template.setDefaultDepositAmount(req.getDefaultDepositAmount());
        template.setDefaultElectricPrice(req.getDefaultElectricPrice());
        template.setDefaultWaterPrice(req.getDefaultWaterPrice());
        template.setDefaultOtherFees(req.getDefaultOtherFees());
        template.setMoveInRules(req.getMoveInRules().trim());
        template.setServiceNotes(req.getServiceNotes().trim());
        template.setAdditionalTerms(blankToNull(req.getAdditionalTerms()));
        template.setContent(ContractTermsFormatter.format(
                template.getMoveInRules(),
                template.getDefaultMonthlyRent(),
                template.getDefaultDepositAmount(),
                template.getDefaultElectricPrice(),
                template.getDefaultWaterPrice(),
                template.getDefaultOtherFees(),
                template.getServiceNotes(),
                template.getAdditionalTerms()
        ));
        return ContractTemplateResponse.from(templateRepo.save(template));
    }

    private void ensureLandlordOrAdmin(UserPrincipal principal) {
        if (principal.getRole() != UserEntity.Role.LANDLORD && principal.getRole() != UserEntity.Role.ADMIN) {
            throw new AppException("Chỉ chủ trọ mới được quản lý mẫu hợp đồng", 403);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
