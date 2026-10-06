package Gabransel.DocFlow.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteOwnUserDto(@NotBlank String password) {
}
