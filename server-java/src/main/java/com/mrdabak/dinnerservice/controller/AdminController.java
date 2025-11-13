package com.mrdabak.dinnerservice.controller;

import com.mrdabak.dinnerservice.dto.AuthRequest;
import com.mrdabak.dinnerservice.dto.AuthResponse;
import com.mrdabak.dinnerservice.dto.UserDto;
import com.mrdabak.dinnerservice.model.User;
import com.mrdabak.dinnerservice.repository.UserRepository;
import com.mrdabak.dinnerservice.service.AuthService;
import com.mrdabak.dinnerservice.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/create-employee")
    public ResponseEntity<?> createEmployee(@Valid @RequestBody AuthRequest request, Authentication authentication) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("error", "User already exists"));
            }

            User employee = new User();
            employee.setEmail(request.getEmail());
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
            employee.setName(request.getName());
            employee.setAddress(request.getAddress() != null ? request.getAddress() : "");
            employee.setPhone(request.getPhone() != null ? request.getPhone() : "");
            employee.setRole("employee");

            User savedEmployee = userRepository.save(employee);
            String token = jwtService.generateToken(savedEmployee.getId(), savedEmployee.getEmail(), savedEmployee.getRole());

            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                    "Employee created successfully",
                    token,
                    new UserDto(savedEmployee.getId(), savedEmployee.getEmail(), savedEmployee.getName(),
                            savedEmployee.getAddress(), savedEmployee.getPhone(), savedEmployee.getRole())
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(user -> new UserDto(user.getId(), user.getEmail(), user.getName(),
                        user.getAddress(), user.getPhone(), user.getRole()))
                .toList());
    }

    @GetMapping("/employees")
    public ResponseEntity<?> getEmployees() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(user -> "employee".equals(user.getRole()))
                .map(user -> new UserDto(user.getId(), user.getEmail(), user.getName(),
                        user.getAddress(), user.getPhone(), user.getRole()))
                .toList());
    }

    @GetMapping("/customers")
    public ResponseEntity<?> getCustomers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(user -> "customer".equals(user.getRole()))
                .map(user -> new UserDto(user.getId(), user.getEmail(), user.getName(),
                        user.getAddress(), user.getPhone(), user.getRole()))
                .toList());
    }
}

