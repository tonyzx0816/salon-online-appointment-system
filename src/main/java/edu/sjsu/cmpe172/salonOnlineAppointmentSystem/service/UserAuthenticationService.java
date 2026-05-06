package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.security.SalonUserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAuthenticationService implements UserDetailsService {
    private final UserRepository userRepository;

    public UserAuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String rawLogin) throws UsernameNotFoundException {
        if (rawLogin == null || rawLogin.isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }
        String email = rawLogin.trim();
        UserEntity user = userRepository.findByEmail(email)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new SalonUserPrincipal(user);
    }
}
