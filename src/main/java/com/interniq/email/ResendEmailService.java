package com.interniq.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "application.email.provider", havingValue = "resend")
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final RestClient restClient = RestClient.create("https://api.resend.com");

    @Value("${application.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${application.email.from}")
    private String fromEmail;

    @Value("${application.email.fail-on-error:false}")
    private boolean failOnError;

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        if (!emailEnabled) {
            log.info("Email disabled. Password reset link for {}: {}", to, resetLink);
            return;
        }

        try {
            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(to),
                    "subject", "Reset your InternIQ password",
                    "html", """
                            <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                                <h2>Reset your InternIQ password</h2>
                                <p>Click the button below to reset your password:</p>
                                <p>
                                    <a href="%s"
                                       style="background: #2563eb; color: white; padding: 10px 16px; text-decoration: none; border-radius: 6px; display: inline-block;">
                                       Reset Password
                                    </a>
                                </p>
                                <p>If the button does not work, copy this link:</p>
                                <p>%s</p>
                                <p>If you did not request this, ignore this email.</p>
                            </div>
                            """.formatted(resetLink, resetLink)
            );

            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Password reset email sent to {}", to);
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}", to, ex);

            if (failOnError) {
                throw new IllegalStateException("Failed to send password reset email", ex);
            }
        }
    }
}
