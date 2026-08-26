package Gabransel.RegisterLogin.services;

import Gabransel.RegisterLogin.dto.FileResponseDto;
import Gabransel.RegisterLogin.entities.File;
import Gabransel.RegisterLogin.entities.User;
import Gabransel.RegisterLogin.repositories.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
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

    return new FileResponseDto(
            savedFile.getName(),
            savedFile.getStatus(),
            savedFile.getId(),
            savedFile.getCreatedAt(),
            savedFile.getType()
    );
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

