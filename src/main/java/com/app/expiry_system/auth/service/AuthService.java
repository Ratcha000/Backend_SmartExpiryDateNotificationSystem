package com.app.expiry_system.auth.service;

import com.app.expiry_system.auth.dto.AuthResponse;
import com.app.expiry_system.auth.dto.LoginRequest;
import com.app.expiry_system.auth.dto.RegisterRequest;
import com.app.expiry_system.auth.dto.UserResponse;
import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.repository.AppUserRepository;
import com.app.expiry_system.auth.security.AppUserPrincipal;
import com.app.expiry_system.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName().trim());
        user.setRole(request.getRole());

        if (request.getRestaurantId() != null && !request.getRestaurantId().isBlank()) {
            throw new IllegalArgumentException("Restaurant cannot be assigned during registration");
        }

        AppUser savedUser = appUserRepository.save(user);
        return new AuthResponse(jwtService.generateToken(savedUser), toUserResponse(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return new AuthResponse(jwtService.generateToken(user), toUserResponse(user));
    }

    public UserResponse getCurrentUser(AppUserPrincipal principal) {
        return toUserResponse(principal.getUser());
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getRestaurantId());
    }
}
