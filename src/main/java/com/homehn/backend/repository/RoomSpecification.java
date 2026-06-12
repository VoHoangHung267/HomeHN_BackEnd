package com.homehn.backend.repository;

import com.homehn.backend.entity.RoomEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RoomSpecification {
    private static final Set<String> LOCATION_STOP_WORDS = Set.of(
            "tim", "kiem", "phong", "tro", "nha", "can", "o", "thue",
            "gan", "quanh", "khu", "vuc", "tai", "duoi", "tren", "muc",
            "gia", "cho", "nguoi", "sinh", "vien", "dai", "hoc", "truong",
            "hocvien", "co", "so", "cs"
    );

    public static Specification<RoomEntity> filter(
            String keyword,
            String ward,
            BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minArea, BigDecimal maxArea,
            RoomEntity.RoomType roomType,
            Boolean isFurnished,
            RoomEntity.GenderRequirement genderRequirement
    ) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            predicates.add(root.get("status").in(
                    RoomEntity.RoomStatus.ACTIVE,
                    RoomEntity.RoomStatus.AVAILABLE_SOON
            ));

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(buildKeywordPredicate(keyword, root, cb));
            }
            if (ward != null && !ward.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("ward")),
                        "%" + ward.toLowerCase(Locale.ROOT) + "%"));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (minArea != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), minArea));
            }
            if (maxArea != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), maxArea));
            }
            if (roomType != null) {
                predicates.add(cb.equal(root.get("roomType"), roomType));
            }
            if (isFurnished != null) {
                predicates.add(cb.equal(root.get("isFurnished"), isFurnished));
            }
            if (genderRequirement != null) {
                predicates.add(cb.equal(root.get("genderRequirement"), genderRequirement));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate buildKeywordPredicate(
            String keyword,
            jakarta.persistence.criteria.Root<RoomEntity> root,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        Set<String> searchTerms = expandKeyword(keyword);
        List<Predicate> orPredicates = new ArrayList<>();

        for (String term : searchTerms) {
            String like = "%" + term.toLowerCase(Locale.ROOT) + "%";
            orPredicates.add(cb.like(cb.lower(root.get("title")), like));
            orPredicates.add(cb.like(cb.lower(root.get("description")), like));
            orPredicates.add(cb.like(cb.lower(root.get("address")), like));
            orPredicates.add(cb.like(cb.lower(root.get("ward")), like));
            orPredicates.add(cb.like(cb.lower(root.get("city")), like));
        }

        return cb.or(orPredicates.toArray(Predicate[]::new));
    }

    private static Set<String> expandKeyword(String keyword) {
        Set<String> terms = new LinkedHashSet<>();
        String trimmed = keyword.trim();
        if (trimmed.isBlank()) {
            return terms;
        }

        terms.add(trimmed);
        terms.addAll(extractMeaningfulTerms(trimmed));
        String normalized = normalize(trimmed);

        addIfContains(normalized, terms,
                List.of("bach khoa", "bkhn", "dai hoc bach khoa"),
                List.of("Bách Khoa", "Hai Bà Trưng", "Lê Thanh Nghị", "Trần Đại Nghĩa"));
        addIfContains(normalized, terms,
                List.of("kinh te quoc dan", "neu", "dai hoc kinh te quoc dan"),
                List.of("Kinh tế Quốc dân", "Hai Bà Trưng", "Giải Phóng", "Trần Đại Nghĩa"));
        addIfContains(normalized, terms,
                List.of("xay dung", "nuce", "dai hoc xay dung"),
                List.of("Xây dựng", "Hai Bà Trưng", "Giải Phóng", "Trần Đại Nghĩa"));
        addIfContains(normalized, terms,
                List.of("ngoai thuong", "ftu", "dai hoc ngoai thuong"),
                List.of("Ngoại thương", "Đống Đa", "Chùa Láng", "Láng Thượng"));
        addIfContains(normalized, terms,
                List.of("hoc vien ngoai giao", "dav", "ngoai giao"),
                List.of("Học viện Ngoại giao", "Đống Đa", "Chùa Láng", "Láng Thượng"));
        addIfContains(normalized, terms,
                List.of("quoc gia", "dhqg", "vnu", "dai hoc quoc gia"),
                List.of("Đại học Quốc gia", "Cầu Giấy", "Xuân Thủy", "Dịch Vọng Hậu"));
        addIfContains(normalized, terms,
                List.of("su pham", "hnue", "dai hoc su pham"),
                List.of("Sư phạm", "Cầu Giấy", "Xuân Thủy", "Dịch Vọng Hậu"));
        addIfContains(normalized, terms,
                List.of("thuong mai", "vcu", "dai hoc thuong mai"),
                List.of("Thương mại", "Cầu Giấy", "Mai Dịch", "Hồ Tùng Mậu"));
        addIfContains(normalized, terms,
                List.of("cong nghiep", "haui", "dai hoc cong nghiep"),
                List.of("Công nghiệp", "Bắc Từ Liêm", "Nhổn", "Minh Khai", "Hồ Tùng Mậu"));
        addIfContains(normalized, terms,
                List.of("giao thong van tai", "utc", "dai hoc giao thong van tai"),
                List.of("Giao thông Vận tải", "Đống Đa", "Láng Thượng", "Cầu Giấy"));
        addIfContains(normalized, terms,
                List.of("fpt", "hoa lac"),
                List.of("FPT", "Hòa Lạc", "Thạch Thất"));

        return terms;
    }

    private static Set<String> extractMeaningfulTerms(String keyword) {
        Set<String> terms = new LinkedHashSet<>();
        String[] originalTokens = keyword.trim().split("[,;/\\\\\\-\\s]+");
        List<String> meaningfulTokens = new ArrayList<>();

        for (String token : originalTokens) {
            String cleaned = token == null ? "" : token.trim();
            if (cleaned.isBlank()) {
                continue;
            }

            String normalizedToken = normalize(cleaned).replaceAll("[^a-z0-9]", "");
            if (normalizedToken.isBlank() || LOCATION_STOP_WORDS.contains(normalizedToken)) {
                continue;
            }

            meaningfulTokens.add(cleaned);
            if (cleaned.length() >= 3) {
                terms.add(cleaned);
            }
        }

        if (!meaningfulTokens.isEmpty()) {
            terms.add(String.join(" ", meaningfulTokens));
        }

        return terms;
    }

    private static void addIfContains(String normalizedKeyword, Set<String> terms, List<String> triggers, List<String> expansions) {
        boolean matched = triggers.stream().anyMatch(normalizedKeyword::contains);
        if (matched) {
            terms.addAll(expansions);
        }
    }

    private static String normalize(String input) {
        String normalized = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }
}
