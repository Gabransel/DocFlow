package Gabransel.DocFlow.exceptions;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String message) {
        super("File not found: " + message);
    }
}
