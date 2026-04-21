package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Person;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.PasswordUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final long CODE_EXPIRY_SECONDS = 15 * 60; // 15 minutes
    private static final int CODE_LENGTH = 6;

    private final PersonRepository personRepository;
    private final PasswordUtil passwordUtil;
    private final EmailService emailService;

    // email -> [code, expiresAt]
    private final ConcurrentHashMap<String, ResetEntry> pendingResets = new ConcurrentHashMap<>();

    public void requestReset(String email) throws ValidationException {
        personRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("No account found with email: " + email));

        String code = generateCode();
        pendingResets.put(email, new ResetEntry(code, Instant.now().plusSeconds(CODE_EXPIRY_SECONDS)));
        emailService.sendPasswordResetCode(email, code);
        log.info("Password reset requested for {}", email);
    }

    public void resetPassword(String email, String code, String newPassword) throws ValidationException {
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("No account found with email: " + email));

        ResetEntry entry = pendingResets.get(email);
        if (entry == null) {
            throw new ValidationException("No password reset was requested for this email.");
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            pendingResets.remove(email);
            throw new ValidationException("The reset code has expired. Please request a new one.");
        }
        if (!entry.code().equals(code)) {
            throw new ValidationException("Invalid reset code.");
        }

        person.setPassword(passwordUtil.hashPassword(newPassword));
        personRepository.save(person);
        pendingResets.remove(email);

        emailService.sendPasswordChangedConfirmation(email);
        log.info("Password successfully reset for {}", email);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", number);
    }

    private record ResetEntry(String code, Instant expiresAt) {
    }
}