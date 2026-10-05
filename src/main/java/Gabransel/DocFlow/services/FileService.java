package Gabransel.DocFlow.services;

import Gabransel.DocFlow.dto.FileResponseDto;
import Gabransel.DocFlow.entities.File;
import Gabransel.DocFlow.entities.User;
import Gabransel.DocFlow.exceptions.FileNotFoundException;
import Gabransel.DocFlow.repositories.FileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final FileProcessingService fileProcessingService;

    public FileService(FileRepository fileRepository, FileProcessingService fileProcessingService) {
        this.fileRepository = fileRepository;
        this.fileProcessingService = fileProcessingService;
    }

    public FileResponseDto uploadFile(MultipartFile file, User user) {

        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        File.FileType type = inferFileType(contentType);

        File newFile = new File(
                originalFileName,
                null,
                null,
                type,
                user
        );

        File savedFile = fileRepository.save(newFile);
        byte[] fileBytes;

        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {

            throw new RuntimeException("Failed to extract file bytes for processing", e);
        }

        fileProcessingService.processFile(savedFile.getId(), fileBytes);

    return new FileResponseDto(
            savedFile.getName(),
            savedFile.getStatus(),
            savedFile.getId(),
            savedFile.getCreatedAt(),
            savedFile.getType()
    );
    }

    public List<FileResponseDto> listMyFiles(User user, File.FileType type){
        List<File> files;

        if (type == null) {
            files = fileRepository.findByUserId(user.getId());
        } else {
            files = fileRepository.findByUserIdAndType(user.getId(), type);
        }

        return files.stream()
                .map(file -> new FileResponseDto(
                        file.getName(),
                        file.getStatus(),
                        file.getId(),
                        file.getCreatedAt(),
                        file.getType()
                ))
                .toList();
    }

    public List<FileResponseDto> listAllFiles(){
        List<File> files = fileRepository.findAll();

        return files.stream()
                .map(file -> new FileResponseDto(
                        file.getName(),
                        file.getStatus(),
                        file.getId(),
                        file.getCreatedAt(),
                        file.getType()
                ))
                .toList();
    }

    public FileResponseDto getFindFileById(Long id, User user) {
        File file = findFileWithAccessCheck(id, user);

        return new FileResponseDto(
                        file.getName(),
                        file.getStatus(),
                        file.getId(),
                        file.getCreatedAt(),
                        file.getType()
                );
    }

    public void deleteFileById(Long id, User user) {
        File file = findFileWithAccessCheck(id, user);

        fileRepository.delete(file);
    }

    public File.FileType inferFileType(String contentType){
        if(contentType == null){
            return File.FileType.OTHER;
        }

        if (contentType.startsWith("image/")){
            return File.FileType.IMAGE;
        } else if (contentType.startsWith("video/")){
            return File.FileType.VIDEO;
        } else if (contentType.startsWith("application/pdf")){
            return File.FileType.DOC;
        }

        return File.FileType.OTHER;
    }

    private File findFileWithAccessCheck(Long id, User user){
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("Id: " + id));

        boolean isOwner = file.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().contains(User.UserRole.ADMIN);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to access this file.");
        }
        return file;
    }
}

