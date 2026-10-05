package Gabransel.DocFlow.dto;

import Gabransel.DocFlow.entities.File;


import java.time.LocalDateTime;

public record FileResponseDto(String name, File.FileStatus status, Long id, LocalDateTime createdAt, File.FileType type) {
}
