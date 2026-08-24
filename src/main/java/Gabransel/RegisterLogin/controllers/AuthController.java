package Gabransel.RegisterLogin.controllers;

import Gabransel.RegisterLogin.dto.LoginRequestDto;
import Gabransel.RegisterLogin.dto.LoginResponseDto;
import Gabransel.RegisterLogin.dto.RegisterRequestDto;
import Gabransel.RegisterLogin.dto.RegisterResponseDto;
import Gabransel.RegisterLogin.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/auth")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> insert(@Valid @RequestBody RegisterRequestDto dto) {
        RegisterResponseDto newUser = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        LoginResponseDto response = authService.login(dto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
