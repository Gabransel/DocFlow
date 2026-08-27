package Gabransel.RegisterLogin.services;

import Gabransel.RegisterLogin.dto.FileResponseDto;
import Gabransel.RegisterLogin.entities.File;
import Gabransel.RegisterLogin.entities.User;
import Gabransel.RegisterLogin.repositories.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
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

            throw new RuntimeException("Falha ao extrair os bytes do arquivo para processamento", e);
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

    public List<FileResponseDto> listMyFiles(User user){
        List<File> files = fileRepository.findByUserId(user.getId());

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
}

