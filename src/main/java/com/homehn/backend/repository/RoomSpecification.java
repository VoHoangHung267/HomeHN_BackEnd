package com.homehn.backend.repository;

import com.homehn.backend.entity.RoomEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;

public class RoomSpecification {

    public static Specification<RoomEntity> filter(
            String keyword,
            String district,
            BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minArea,  BigDecimal maxArea,
            RoomEntity.RoomType roomType,
            Boolean isFurnished,
            RoomEntity.GenderRequirement genderRequirement
    ) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            // Chỉ lấy phòng ACTIVE
            predicates.add(cb.equal(root.get("status"), RoomEntity.RoomStatus.ACTIVE));

            if (keyword != null && !keyword.isBlank()) {
                var kw = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")),       kw),
                        cb.like(cb.lower(root.get("description")), kw),
                        cb.like(cb.lower(root.get("address")),     kw),
                        cb.like(cb.lower(root.get("district")),    kw)
                ));
            }
            if (district != null && !district.isBlank())
                predicates.add(cb.like(cb.lower(root.get("district")),
                        "%" + district.toLowerCase() + "%"));

            if (minPrice != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (minArea != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), minArea));
            if (maxArea != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), maxArea));
            if (roomType != null)
                predicates.add(cb.equal(root.get("roomType"), roomType));
            if (isFurnished != null)
                predicates.add(cb.equal(root.get("isFurnished"), isFurnished));
            if (genderRequirement != null)
                predicates.add(cb.equal(root.get("genderRequirement"), genderRequirement));

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
