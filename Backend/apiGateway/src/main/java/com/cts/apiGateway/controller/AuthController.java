package com.cts.apiGateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.apiGateway.dto.AuthRequest;
import com.cts.apiGateway.dto.AuthResponse;
import com.cts.apiGateway.dto.RegisterRequest;
import com.cts.apiGateway.dto.CustomerSignupRequest;
import com.cts.apiGateway.dto.CustomerAuthResponse;
import com.cts.apiGateway.model.Role;
import com.cts.apiGateway.model.User;
import com.cts.apiGateway.repository.UserRepository;
import com.cts.apiGateway.security.JwtUtil;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user.
     * SuperAdmin signup is not allowed via this endpoint (seeded in DB).
     * FraudAnalyst / RiskManager are created with isApproved=false until SuperAdmin approves.
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@RequestBody RegisterRequest request) {

        Role roleEnum;
        try {
            roleEnum = Role.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message("Invalid role. Allowed: FRAUD_ANALYST, RISK_MANAGER")
                            .build()
            ));
        }

        if (roleEnum == Role.SUPER_ADMIN) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    AuthResponse.builder()
                            .message("SUPER_ADMIN signup is not allowed.")
                            .build()
            ));
        }

        return userRepository.findByUsername(request.getUsername())
                .flatMap(existing -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(
                        AuthResponse.builder()
                                .message("Username already exists")
                                .build()
                )))
                .switchIfEmpty(
                        userRepository.save(
                                User.builder()
                                        .username(request.getUsername())
                                        .password(passwordEncoder.encode(request.getPassword()))
                                        .role(request.getRole())
                                        .isApproved(false)
                                        .build()
                        ).map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(
                                AuthResponse.builder()
                                        .username(saved.getUsername())
                                        .role(saved.getRole())
                                        .message("Signup submitted. Wait for SuperAdmin approval.")
                                        .build()
                        ))
                );
    }

    /**
     * Login. Blocks users that are not yet approved.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                AuthResponse.builder().message("Invalid username or password").build()
                        ));
                    }

                    boolean approved = Boolean.TRUE.equals(user.getIsApproved())
                            || "SUPER_ADMIN".equals(user.getRole());

                    if (!approved) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                AuthResponse.builder()
                                        .message("Account pending SuperAdmin approval.")
                                        .build()
                        ));
                    }

                    String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                    return Mono.just(ResponseEntity.ok(
                            AuthResponse.builder()
                                    .token(token)
                                    .username(user.getUsername())
                                    .role(user.getRole())
                                    .message("Login successful")
                                    .build()
                    ));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        AuthResponse.builder()
                                .message("Invalid username or password")
                                .build()
                )));
    }

    /**
     * Create a new SUPER_ADMIN user.
     * Development/Admin endpoint for creating super admins.
     */
    @PostMapping("/admin/create-superadmin")
    public Mono<ResponseEntity<AuthResponse>> createSuperAdmin(@RequestBody RegisterRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .flatMap(existing -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(
                        AuthResponse.builder()
                                .message("Username already exists")
                                .build()
                )))
                .switchIfEmpty(
                        userRepository.save(
                                User.builder()
                                        .username(request.getUsername())
                                        .password(passwordEncoder.encode(request.getPassword()))
                                        .role("SUPER_ADMIN")
                                        .isApproved(true)
                                        .build()
                        ).map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(
                                AuthResponse.builder()
                                        .username(saved.getUsername())
                                        .role(saved.getRole())
                                        .message("SUPER_ADMIN created successfully")
                                        .build()
                        ))
                );
    }

    /**
     * Customer Signup with JWT
     * Creates a new customer account and issues a JWT token
     */
    @PostMapping("/customer/signup")
    public Mono<ResponseEntity<CustomerAuthResponse>> customerSignup(@RequestBody CustomerSignupRequest request) {
        // Validate email uniqueness
        return userRepository.findByEmail(request.getEmail())
                .flatMap(existing -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(
                        CustomerAuthResponse.builder()
                                .message("Email already registered")
                                .build()
                )))
                .switchIfEmpty(
                        // Also check username if email is used as username
                        userRepository.save(
                                User.builder()
                                        .username(request.getEmail()) // Use email as username
                                        .email(request.getEmail())
                                        .password(passwordEncoder.encode(request.getPassword()))
                                        .role("CUSTOMER")
                                        .isApproved(true) // Customers are auto-approved
                                        .bankName(request.getBankName())
                                        .accountNo(request.getAccountNo())
                                        .accountType(request.getAccountType())
                                        .balance(request.getBalance() != null ? request.getBalance() : 0.0)
                                        .riskScore(0.0)
                                        .build()
                        ).map(saved -> {
                            String token = jwtUtil.generateToken(saved.getUsername(), saved.getRole());
                            return ResponseEntity.status(HttpStatus.CREATED).body(
                                    CustomerAuthResponse.builder()
                                            .token(token)
                                            .email(saved.getEmail())
                                            .username(saved.getUsername())
                                            .name(request.getBankName())
                                            .bankName(saved.getBankName())
                                            .accountNo(saved.getAccountNo())
                                            .accountType(saved.getAccountType())
                                            .balance(saved.getBalance())
                                            .riskScore(saved.getRiskScore())
                                            .role(saved.getRole())
                                            .message("Customer account created successfully")
                                            .build()
                            );
                        })
                );
    }

    /**
     * Customer Login with JWT
     * Authenticates customer and issues JWT token
     */
    @PostMapping("/customer/login")
    public Mono<ResponseEntity<CustomerAuthResponse>> customerLogin(@RequestBody AuthRequest request) {
        // Try to find customer by email (assuming email is used as username for customers)
        return userRepository.findByEmail(request.getUsername())
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                CustomerAuthResponse.builder()
                                        .message("Invalid email or password")
                                        .build()
                        ));
                    }

                    if (!"CUSTOMER".equals(user.getRole())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                CustomerAuthResponse.builder()
                                        .message("Invalid credentials")
                                        .build()
                        ));
                    }

                    String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                    return Mono.just(ResponseEntity.ok(
                            CustomerAuthResponse.builder()
                                    .token(token)
                                    .email(user.getEmail())
                                    .username(user.getUsername())
                                    .bankName(user.getBankName())
                                    .accountNo(user.getAccountNo())
                                    .accountType(user.getAccountType())
                                    .balance(user.getBalance())
                                    .riskScore(user.getRiskScore())
                                    .role(user.getRole())
                                    .message("Login successful")
                                    .build()
                    ));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        CustomerAuthResponse.builder()
                                .message("Invalid email or password")
                                .build()
                )));
    }
}
