package Gabransel.DocFlow.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(@NotBlank String name, @NotBlank @Email String email, @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$") String password) {
}