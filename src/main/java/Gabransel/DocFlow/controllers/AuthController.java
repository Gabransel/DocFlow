package Gabransel.DocFlow.controllers;

import Gabransel.DocFlow.dto.LoginRequestDto;
import Gabransel.DocFlow.dto.LoginResponseDto;
import Gabransel.DocFlow.dto.RegisterRequestDto;
import Gabransel.DocFlow.dto.RegisterResponseDto;
import Gabransel.DocFlow.services.AuthService;
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
