package com.vassarlabs.aulm.service;

import com.vassarlabs.aulm.dto.ForgotPasswordRequest;
import com.vassarlabs.aulm.dto.ForgotPasswordResponse;
import com.vassarlabs.aulm.dto.ResetPasswordRequest;
import com.vassarlabs.aulm.exception.ApiException;
import com.vassarlabs.aulm.model.PasswordResetToken;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.repository.PasswordResetTokenRepository;
import com.vassarlabs.aulm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    private static final long TOKEN_EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final String appBaseUrl;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordResetTokenRepository tokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 JavaMailSender mailSender,
                                 @Value("${aulm.mail.from}") String mailFrom,
                                 @Value("${aulm.app-base-url}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.appBaseUrl = appBaseUrl;
    }

    public ForgotPasswordResponse requestReset(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByUsernameAndProjectName(request.username(), request.projectName());
        User user = userOpt.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                "No account found with that username, project, and email."));

        if (user.getEmail() == null || user.getEmail().isBlank()
                || !user.getEmail().equalsIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "No account found with that username, project, and email.");
        }

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        tokenRepository.save(resetToken);

        sendResetEmail(user, resetToken.getToken());

        return new ForgotPasswordResponse(user.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));
        if (!resetToken.isValid()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private void sendResetEmail(User user, String token) {
        String link = appBaseUrl + "/?resetToken=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Reset your AULM password");
        message.setText("A password reset was requested for your AULM account (username: " + user.getUsername()
                + ", project: " + user.getProjectName() + ").\n\n"
                + "Click the link below to set a new password. This link expires in " + TOKEN_EXPIRY_MINUTES + " minutes:\n\n"
                + link + "\n\n"
                + "If you didn't request this, you can safely ignore this email.");
        mailSender.send(message);
    }
}
