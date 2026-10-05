package Gabransel.DocFlow.dto;

import jakarta.validation.constraints.Email;


public record UpdateUserDto(String name, @Email String email) {
}
