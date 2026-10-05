package Gabransel.DocFlow.dto;

import Gabransel.DocFlow.entities.User;
import jakarta.validation.constraints.NotNull;


public record UpdateRoleDto(@NotNull User.UserRole role) {
}
