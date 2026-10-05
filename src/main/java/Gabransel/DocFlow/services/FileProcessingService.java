package Gabransel.DocFlow.services;

import Gabransel.DocFlow.entities.File;
import Gabransel.DocFlow.repositories.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class FileProcessingService {

    private final FileRepository fileRepository;

    private final S3Client s3Client;

    public FileProcessingService(FileRepository fileRepository, S3Client s3Client) {
        this.fileRepository = fileRepository;
        this.s3Client = s3Client;
    }

    @Value("${aws.s3.bucket}")
    private String bucketName;


    @Async("taskExecutor")
    public void processFile(Long fileId, byte[] fileBytes) {

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found for processing. ID: " + fileId));
        try {

            Thread.sleep(3000);


            String fileHash = generateHash(fileBytes);

            System.out.println("--- Starting file processing ID: " + fileId + " ---");
            System.out.println("Generated hash: " + fileHash);
            System.out.println("Verification complete: APPROVED!");

            String s3Key = "files/" + file.getUser().getId() + "/" + file.getId() + "-" + file.getName();

            file.setS3Key(s3Key);
            file.setFileHash(fileHash);

            RequestBody requestBody = RequestBody.fromBytes(fileBytes);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.putObject(putRequest, requestBody);

            file.setStatus(File.FileStatus.PROCESSED);

            fileRepository.save(file);
            System.out.println("--- File ID: " + fileId + " successfully saved as PROCESSED ---");

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            System.err.println("The simulated processing was interrupted.: " + e.getMessage());


            file.setStatus(File.FileStatus.FAILED);
            fileRepository.save(file);

        } catch (Exception e) {

            System.err.println("Unexpected error while processing file ID " + fileId + ": " + e.getMessage());

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
            throw new RuntimeException("Error generating hash", e);
        }
    }
}
