package Gabransel.DocFlow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordDto(@NotBlank String currentPassword, @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$") String newPassword) {
}
