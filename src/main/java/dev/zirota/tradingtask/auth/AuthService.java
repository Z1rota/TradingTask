package dev.zirota.tradingtask.auth;

import dev.zirota.tradingtask.auth.dto.LoginRequest;
import dev.zirota.tradingtask.auth.dto.LoginResponse;
import dev.zirota.tradingtask.auth.dto.RegisterRequest;
import dev.zirota.tradingtask.auth.dto.RegisterResponse;
import dev.zirota.tradingtask.user.User;
import dev.zirota.tradingtask.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.username().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        User user = new User(username, passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return new RegisterResponse(user.getId(), user.getUsername());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().toLowerCase(Locale.ROOT);
        User user = userRepository.findByUsername(username)
                .filter(existing -> passwordEncoder.matches(request.password(), existing.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        return jwtService.createToken(user.getId());
    }
}
