package com.interniq.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service("passwordResetSmtpEmailService")
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Reset your InternIQ password");

        message.setText("""
                Hello,

                We received a request to reset your InternIQ password.

                Click the link below to reset your password:

                %s

                This link will expire in 1 minutes.

                If you did not request this password reset, please take action as soon as possible this email.

                Regards,
                InternIQ Team
                """.formatted(resetLink));

        mailSender.send(message);
    }
}