package com.andrei.demo.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Code");
        message.setText("Your password reset code is: " + code +
                "\n\nThis code expires in 15 minutes." +
                "\n\nIf you did not request a password reset, please ignore this email.");
        mailSender.send(message);
        log.info("Password reset code sent to {}", toEmail);
    }

    public void sendPasswordChangedConfirmation(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Changed Successfully");
        message.setText("Your password has been changed successfully." +
                "\n\nIf you did not make this change, please contact support immediately.");
        mailSender.send(message);
        log.info("Password change confirmation sent to {}", toEmail);
    }
}