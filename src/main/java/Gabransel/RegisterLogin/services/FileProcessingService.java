package Gabransel.RegisterLogin.services;

import Gabransel.RegisterLogin.entities.File;
import Gabransel.RegisterLogin.repositories.FileRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class FileProcessingService {

    private final FileRepository fileRepository;

    public FileProcessingService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Async("taskExecutor")
    public void processFile(Long fileId, byte[] fileBytes) {

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado para processamento. ID: " + fileId));
        try {

            Thread.sleep(3000);


            String fileHash = generateHash(fileBytes);

            System.out.println("--- Iniciando processamento do Arquivo ID: " + fileId + " ---");
            System.out.println("Hash gerado: " + fileHash);
            System.out.println("Verificação concluída: APROVADO!");


            file.setS3Key("local/" + file.getId());
            file.setFileHash(fileHash);
            file.setStatus(File.FileStatus.PROCESSED);


            fileRepository.save(file);
            System.out.println("--- Arquivo ID: " + fileId + " salvo com sucesso como PROCESSED ---");

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            System.err.println("O processamento simulado foi interrompido: " + e.getMessage());


            file.setStatus(File.FileStatus.FAILED);
            fileRepository.save(file);

        } catch (Exception e) {

            System.err.println("Erro inesperado ao processar arquivo ID " + fileId + ": " + e.getMessage());

            file.setStatus(File.FileStatus.FAILED);
            fileRepository.save(file);
        }
    }


    private String generateHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash", e);
        }
    }
}
