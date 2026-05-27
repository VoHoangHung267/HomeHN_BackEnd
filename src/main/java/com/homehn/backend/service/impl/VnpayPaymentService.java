package com.homehn.backend.service.impl;

import com.homehn.backend.config.VnpayProperties;
import com.homehn.backend.entity.RentalBookingEntity;
import com.homehn.backend.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class VnpayPaymentService {

    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties vnpayProperties;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public PaymentCreationResult createDepositPayment(RentalBookingEntity booking, HttpServletRequest request) {
        ensureConfigured();

        String txnRef = buildTxnRef(booking);
        long amount = toVnpayAmount(booking.getDepositAmount());
        LocalDateTime now = ZonedDateTime.now(APP_ZONE).toLocalDateTime();

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnpayProperties.getVersion());
        params.put("vnp_Command", vnpayProperties.getCommand());
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", vnpayProperties.getCurrCode());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", buildOrderInfo(booking));
        params.put("vnp_OrderType", vnpayProperties.getOrderType());
        params.put("vnp_Locale", vnpayProperties.getLocale());
        params.put("vnp_ReturnUrl", backendUrl + "/bookings/vnpay/return");
        params.put("vnp_IpAddr", resolveIpAddress(request));
        params.put("vnp_CreateDate", now.format(VNPAY_DATE_FORMAT));
        params.put("vnp_ExpireDate", now.plusMinutes(vnpayProperties.getExpireMinutes()).format(VNPAY_DATE_FORMAT));

        String query = buildQuery(params);
        String secureHash = hmacSha512(query, vnpayProperties.getHashSecret());
        String payUrl = vnpayProperties.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;

        return PaymentCreationResult.builder()
                .requestId(txnRef)
                .orderId(txnRef)
                .payUrl(payUrl)
                .message("Đã tạo link thanh toán VNPAY")
                .resultCode(0)
                .build();
    }

    public CallbackVerificationResult verifyCallback(Map<String, String> input) {
        ensureConfigured();

        String receivedHash = input.get("vnp_SecureHash");
        if (isBlank(receivedHash)) {
            return CallbackVerificationResult.invalid("Thiếu chữ ký VNPAY");
        }

        Map<String, String> params = new TreeMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!isBlank(key) && key.startsWith("vnp_") && !isBlank(value)
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)) {
                params.put(key, value);
            }
        }

        String expectedHash = hmacSha512(buildQuery(params), vnpayProperties.getHashSecret());
        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            return CallbackVerificationResult.invalid("Sai chữ ký VNPAY");
        }

        String txnRef = params.get("vnp_TxnRef");
        long amount = parseLong(params.get("vnp_Amount"), -1L);
        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "");
        boolean success = "00".equals(responseCode) && ("00".equals(transactionStatus) || transactionStatus.isBlank());

        return CallbackVerificationResult.builder()
                .valid(true)
                .txnRef(txnRef)
                .amount(amount)
                .responseCode(responseCode)
                .transactionStatus(transactionStatus)
                .transactionNo(parseLong(params.get("vnp_TransactionNo"), -1L))
                .bankCode(params.get("vnp_BankCode"))
                .message(resolvePaymentMessage(responseCode, transactionStatus, success))
                .success(success)
                .build();
    }

    public String buildFrontendReturnUrl(Long bookingId, boolean success) {
        return UriComponentsBuilder.fromUriString(frontendUrl + "/bookings/" + bookingId)
                .queryParam("payment", "returned")
                .queryParam("result", success ? "success" : "failed")
                .build(true)
                .toUriString();
    }

    public String buildFrontendReturnUrlForList(boolean success) {
        return UriComponentsBuilder.fromUriString(frontendUrl + "/bookings")
                .queryParam("payment", "returned")
                .queryParam("result", success ? "success" : "failed")
                .build(true)
                .toUriString();
    }

    public boolean isEnabled() {
        return vnpayProperties.isEnabled();
    }

    public int getExpireMinutes() {
        return vnpayProperties.getExpireMinutes();
    }

    private void ensureConfigured() {
        if (!vnpayProperties.isEnabled()
                || isBlank(vnpayProperties.getTmnCode())
                || isBlank(vnpayProperties.getHashSecret())) {
            throw new AppException("VNPAY sandbox chưa được cấu hình. Vui lòng thêm vnp_TmnCode và vnp_HashSecret.");
        }
    }

    private String buildTxnRef(RentalBookingEntity booking) {
        return "BOOKING" + booking.getId() + System.currentTimeMillis();
    }

    private String buildOrderInfo(RentalBookingEntity booking) {
        return "Dat coc phong " + booking.getContractCode();
    }

    private long toVnpayAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().longValueExact() * 100L;
    }

    private String buildQuery(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                query.append('&');
            }
            query.append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(entry.getValue()));
            first = false;
        }
        return query.toString();
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (!isBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (!isBlank(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String hmacSha512(String data, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new AppException("Không tạo được chữ ký VNPAY: " + ex.getMessage());
        }
    }

    private long parseLong(String value, long fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolvePaymentMessage(String responseCode, String transactionStatus, boolean success) {
        if (success) {
            return "Thanh toán thành công qua VNPAY";
        }

        String code = !isBlank(responseCode) ? responseCode : transactionStatus;
        return switch (code) {
            case "15" -> "Giao dịch đã hết thời gian chờ thanh toán trên VNPAY";
            case "24" -> "Giao dịch đã bị hủy trên VNPAY";
            default -> "Thanh toán VNPAY không thành công";
        };
    }

    @Getter
    @Builder
    public static class PaymentCreationResult {
        private String requestId;
        private String orderId;
        private String payUrl;
        private String message;
        private int resultCode;
    }

    @Getter
    @Builder
    public static class CallbackVerificationResult {
        private boolean valid;
        private boolean success;
        private String txnRef;
        private long amount;
        private String responseCode;
        private String transactionStatus;
        private long transactionNo;
        private String bankCode;
        private String message;

        public static CallbackVerificationResult invalid(String message) {
            return CallbackVerificationResult.builder()
                    .valid(false)
                    .success(false)
                    .amount(-1L)
                    .transactionNo(-1L)
                    .message(message)
                    .build();
        }
    }
}
