package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.dto.auth.LoginRequest;
import dev.zirota.tradingtask.dto.auth.LoginResponse;
import dev.zirota.tradingtask.dto.auth.RegisterRequest;
import dev.zirota.tradingtask.dto.auth.RegisterResponse;
import dev.zirota.tradingtask.entity.User;
import dev.zirota.tradingtask.exception.UsernameAlreadyExistsException;
import dev.zirota.tradingtask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.username().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        User user = new User(username, passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return new RegisterResponse(user.getId(), user.getUsername());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().toLowerCase();
        User user = userRepository.findByUsername(username)
                .filter(existing -> passwordEncoder.matches(request.password(), existing.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        return jwtService.createToken(user.getId());
    }
}
