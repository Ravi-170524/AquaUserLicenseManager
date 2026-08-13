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
        User user = userRepository.findByUsernameAndProjectName(request.username(), request.projectName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username, project or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username, project or password");
        }
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been disabled");
        }

        return issueTokenFor(user);
    }

    /**
     * License validity is intentionally not checked here: any enabled account can log in.
     * Whether a user has a valid license only determines what they can see/do afterward
     * (e.g. an unlicensed user lands on a "request access" screen instead of the dashboard).
     */
    public LoginResponse issueTokenFor(User user) {
        String token = jwtUtil.generateToken(user.getUuid());
        return new LoginResponse(token, UserResponse.from(user));
    }
}
