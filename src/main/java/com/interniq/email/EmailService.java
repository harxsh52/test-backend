package com.interniq.email;

public interface EmailService {

    void sendPasswordResetEmail(String to, String resetLink);
}