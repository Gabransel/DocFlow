package Gabransel.RegisterLogin.controllers;

import Gabransel.RegisterLogin.dto.FileResponseDto;
import Gabransel.RegisterLogin.entities.User;
import Gabransel.RegisterLogin.security.UserPrincipal;
import Gabransel.RegisterLogin.services.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponseDto> upload(@RequestParam("file") MultipartFile file,
                                                  @AuthenticationPrincipal UserPrincipal userPrincipal){
        User user = userPrincipal.getUser();
        FileResponseDto response = fileService.uploadFile(file, user);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
