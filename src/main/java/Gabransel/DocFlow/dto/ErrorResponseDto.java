package Gabransel.DocFlow.dto;

public record ErrorResponseDto(String timestamp, int status, String error, String message) {
}
