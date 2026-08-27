package Gabransel.RegisterLogin.controllers;

import Gabransel.RegisterLogin.dto.FileResponseDto;
import Gabransel.RegisterLogin.entities.File;
import Gabransel.RegisterLogin.entities.User;
import Gabransel.RegisterLogin.security.UserPrincipal;
import Gabransel.RegisterLogin.services.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<FileResponseDto>>  list(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                        @RequestParam(required = false) File.FileType type){

        User user = userPrincipal.getUser();
        List<FileResponseDto> files = fileService.listMyFiles(user, type);

        return ResponseEntity.status(HttpStatus.OK).body(files);

    }

    @GetMapping("/{id}")
    public ResponseEntity<FileResponseDto> find(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userPrincipal.getUser();
        FileResponseDto file = fileService.getFindFileById(id, user);

        return ResponseEntity.status(HttpStatus.OK).body(file);
    }

    @GetMapping("/all")
    public ResponseEntity<List<FileResponseDto>> listAll() {
        List<FileResponseDto> files = fileService.listAllFiles();
        return ResponseEntity.status(HttpStatus.OK).body(files);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userPrincipal.getUser();
        fileService.deleteFileById(id, user);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
