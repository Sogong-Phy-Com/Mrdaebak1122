package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.dto.AuthRequest;
import com.mrdabak.dinnerservice.dto.AuthResponse;
import com.mrdabak.dinnerservice.dto.UserDto;
import com.mrdabak.dinnerservice.model.User;
import com.mrdabak.dinnerservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User already exists");
        }

        // Validate role
        String role = request.getRole();
        if (role == null || role.isEmpty()) {
            role = "customer"; // Default to customer
        }
        if (!role.equals("customer") && !role.equals("employee") && !role.equals("admin")) {
            throw new RuntimeException("Invalid role. Must be customer, employee, or admin");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setAddress(request.getAddress());
        user.setPhone(request.getPhone());
        user.setRole(role);

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        return new AuthResponse(
                "User registered successfully",
                token,
                new UserDto(savedUser.getId(), savedUser.getEmail(), savedUser.getName(),
                        savedUser.getAddress(), savedUser.getPhone(), savedUser.getRole())
        );
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        return new AuthResponse(
                "Login successful",
                token,
                new UserDto(user.getId(), user.getEmail(), user.getName(),
                        user.getAddress(), user.getPhone(), user.getRole())
        );
    }
}

