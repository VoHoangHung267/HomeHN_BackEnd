package com.homehn.backend.util;

import java.math.BigDecimal;

public final class ContractTermsFormatter {

    private ContractTermsFormatter() {
    }

    public static String format(
            String moveInRules,
            BigDecimal monthlyRent,
            BigDecimal depositAmount,
            BigDecimal electricPrice,
            BigDecimal waterPrice,
            BigDecimal otherFees,
            String serviceNotes,
            String additionalTerms
    ) {
        StringBuilder builder = new StringBuilder();
        append(builder, "Giờ giấc", moveInRules);
        append(builder, "Giá thuê", money(monthlyRent));
        append(builder, "Tiền cọc", money(depositAmount));
        append(builder, "Giá điện", money(electricPrice));
        append(builder, "Giá nước", money(waterPrice));
        append(builder, "Phí dịch vụ khác", money(otherFees));
        append(builder, "Dịch vụ", serviceNotes);
        append(builder, "Ghi chú thêm", additionalTerms);
        return builder.toString().trim();
    }

    private static void append(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(label).append(": ").append(value.trim());
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString() + " VND";
    }
}
