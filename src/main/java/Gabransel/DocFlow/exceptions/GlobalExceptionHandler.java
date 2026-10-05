package Gabransel.DocFlow.exceptions;

import Gabransel.DocFlow.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailAlreadyExist(EmailAlreadyExistException ex) {
        ErrorResponseDto body = new ErrorResponseDto(
                 Instant.now().toString(),
                 HttpStatus.CONFLICT.value(),
                 "Conflict",
                 ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleFileNotFound(FileNotFoundException ex) {
        ErrorResponseDto body = new ErrorResponseDto(
                Instant.now().toString(),
                HttpStatus.NOT_FOUND.value(),
                "Not found",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
