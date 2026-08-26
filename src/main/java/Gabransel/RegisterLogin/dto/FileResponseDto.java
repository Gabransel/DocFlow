package Gabransel.RegisterLogin.dto;

import Gabransel.RegisterLogin.entities.File;


import java.time.LocalDateTime;

public record FileResponseDto(String name, File.FileStatus status, Long id, LocalDateTime createdAt, File.FileType type) {
}
