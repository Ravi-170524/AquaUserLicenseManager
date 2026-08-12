package com.vassarlabs.aulm.service;

import com.vassarlabs.aulm.dto.LoginRequest;
import com.vassarlabs.aulm.dto.LoginResponse;
import com.vassarlabs.aulm.dto.UserResponse;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.repository.UserRepository;
import com.vassarlabs.aulm.security.JwtUtil;
import org.springframework.http.HttpStatus;
import com.vassarlabs.aulm.exception.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been disabled");
        }
        if (user.getLicense() == null || !user.getLicense().isValid()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No active license for this account. Contact your administrator.");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token, UserResponse.from(user));
    }
}
