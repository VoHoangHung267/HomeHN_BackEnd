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

                    Bạn vừa yêu cầu �‘ặt lại mật khẩu cho tài khoản HomeHN.
                    Vui lòng m�Ÿ liên kết sau �‘�ƒ tạo mật khẩu m�›i:

                    %s

                    Liên kết có hi�‡u lực trong 30 phút. Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.
                    """.formatted(resetLink));
            mailSender.send(message);
        } catch (MailException e) {
            throw new AppException("Không th�ƒ gửi email �‘ặt lại mật khẩu: " + e.getMessage());
        }
    }
}
