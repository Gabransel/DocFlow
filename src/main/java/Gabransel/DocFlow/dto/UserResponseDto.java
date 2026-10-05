package Gabransel.DocFlow.dto;

import Gabransel.DocFlow.entities.User;

import java.util.Set;

public record UserResponseDto(Long id,String name, String email, Set<User.UserRole> roles) {
    public static UserResponseDto from(User user) {
        return new UserResponseDto(user.getId(), user.getName(), user.getEmail(),
                user.getRoles());
    }
}
