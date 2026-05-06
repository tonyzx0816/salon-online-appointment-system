package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserResponse> register(@RequestBody RegisterRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        String emailNorm = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(emailNorm).isPresent()) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        UserEntity user = userRepository.save(new UserEntity(
                null,
                request.name().trim(),
                emailNorm,
                request.phone(),
                passwordEncoder.encode(request.password()),
                request.role() == null ? UserEntity.Role.CUSTOMER : request.role(),
                LocalDateTime.now()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthUserResponse.from(user));
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        String login = authentication.getName();
        UserEntity user = userRepository.findByEmail(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .orElseThrow(() -> new IllegalArgumentException("Current user not found."));
        return AuthUserResponse.from(user);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    public record RegisterRequest(
            String name,
            String email,
            String phone,
            String password,
            UserEntity.Role role
    ) {
    }

    public record AuthUserResponse(
            Integer userId,
            String name,
            String email,
            String phone,
            UserEntity.Role role
    ) {
        static AuthUserResponse from(UserEntity user) {
            return new AuthUserResponse(
                    user.userId(),
                    user.name(),
                    user.email(),
                    user.phone(),
                    user.role()
            );
        }
    }
}
