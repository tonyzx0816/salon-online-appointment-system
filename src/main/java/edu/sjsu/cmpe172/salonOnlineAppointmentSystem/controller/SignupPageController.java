package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Locale;

@Controller
public class SignupPageController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupPageController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(defaultValue = "CUSTOMER") UserEntity.Role role,
            Model model
    ) {
        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Name, email, and password are required.");
            return "signup";
        }
        String emailNorm = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(emailNorm).isPresent()) {
            model.addAttribute("error", "This email is already registered.");
            return "signup";
        }

        userRepository.save(new UserEntity(
                null,
                name.trim(),
                emailNorm,
                null,
                passwordEncoder.encode(password),
                role,
                LocalDateTime.now()
        ));
        return "redirect:/login?registered";
    }
}
