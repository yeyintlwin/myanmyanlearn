package com.barlarlar.myanmyanlearn.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Myan Myan Learn - Email Verification OTP");
            message.setText(buildOtpEmailContent(otpCode));
            
            mailSender.send(message);
            System.out.println("OTP email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send OTP email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildOtpEmailContent(String otpCode) {
        return String.format("""
            Hello!
            
            Welcome to Myan Myan Learn!
            
            Your email verification code is: %s
            
            This code will expire in 10 minutes.
            
            If you didn't request this code, please ignore this email.
            
            Best regards,
            Myan Myan Learn Team
            """, otpCode);
    }

    public void sendTestEmail(String to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Myan Myan Learn - SMTP Test");
            message.setText("""
                Hello!
                
                This is a test email from Myan Myan Learn.
                
                If you received this email, the SMTP configuration is working correctly!
                
                Best regards,
                Myan Myan Learn Team
                """);
            
            mailSender.send(message);
            System.out.println("Test email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send test email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
