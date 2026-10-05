package Gabransel.DocFlow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Component
public class S3BucketInitializer implements CommandLineRunner {

    private final S3Client s3Client;

    public S3BucketInitializer(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Value("${aws.s3.bucket}")
    private String bucketName;



    @Override
    public void run(String... args) throws Exception {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            System.out.println("Bucket already exists");
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            System.out.println("Bucket created now");
        }
    }
}
