package Gabransel.DocFlow.services;


import Gabransel.DocFlow.dto.LoginRequestDto;
import Gabransel.DocFlow.dto.LoginResponseDto;
import Gabransel.DocFlow.dto.RegisterRequestDto;
import Gabransel.DocFlow.dto.RegisterResponseDto;
import Gabransel.DocFlow.entities.User;
import Gabransel.DocFlow.exceptions.EmailAlreadyExistException;
import Gabransel.DocFlow.repositories.UserRepository;
import Gabransel.DocFlow.security.JwtService;
import Gabransel.DocFlow.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public RegisterResponseDto register(RegisterRequestDto dto) {

        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistException(dto.email());
        }

        Set<User.UserRole> roles = Set.of(User.UserRole.USER);
        String hashedPassword = passwordEncoder.encode(dto.password());

        User newUser = new User(roles,  hashedPassword, dto.email(), dto.name());
        User savedUser = userRepository.save(newUser);

        return new RegisterResponseDto(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }

    public LoginResponseDto login(LoginRequestDto dto) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String email = userPrincipal.getUsername();
        var authorities = userPrincipal.getAuthorities();

        String token = jwtService.generateToken(email, authorities);

        return new LoginResponseDto(token, "Bearer");
    }
}
