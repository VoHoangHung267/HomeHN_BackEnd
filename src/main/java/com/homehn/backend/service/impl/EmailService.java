package com.homehn.backend.service.impl;

import com.homehn.backend.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Đặt lại mật khẩu HomeHN");
            message.setText("""
                    Xin chào,

                    Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản HomeHN.
                    Vui lòng mở liên kết sau để tạo mật khẩu mới:

                    %s

                    Liên kết có hiệu lực trong 30 phút. Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.
                    """.formatted(resetLink));
            mailSender.send(message);
        } catch (MailException e) {
            throw new AppException("Không thể gửi email đặt lại mật khẩu: " + e.getMessage());
        }
    }

    public void sendRegistrationVerificationCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Mã xác thực đăng ký HomeHN");
            message.setText("""
                    Xin chào,

                    Mã xác thực đăng ký tài khoản HomeHN của bạn là: %s

                    Mã có hiệu lực trong 10 phút. Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.
                    """.formatted(code));
            mailSender.send(message);
        } catch (MailException e) {
            throw new AppException("Không thể gửi mã xác thực email: " + e.getMessage());
        }
    }
}
