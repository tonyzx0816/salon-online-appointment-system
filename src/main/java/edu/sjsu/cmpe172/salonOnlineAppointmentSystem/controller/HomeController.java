package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.security.SalonUserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            String greetingName = resolveGreetingName(auth);
            model.addAttribute("greetingName", greetingName);
        }
        return "index";
    }

    private String resolveGreetingName(Authentication auth) {
        if (auth.getPrincipal() instanceof SalonUserPrincipal sup) {
            return sup.getDisplayName();
        }
        return userRepository.findByEmail(auth.getName())
                .or(() -> userRepository.findByEmailIgnoreCase(auth.getName()))
                .map(u -> {
                    String n = u.name();
                    return (n != null && !n.isBlank()) ? n.trim() : u.email();
                })
                .orElse(auth.getName());
    }
}
