package Gabransel.RegisterLogin.entities;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_file")
@EntityListeners(AuditingEntityListener.class)
public class File {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)
    private FileStatus status;
    private String s3Key;
    private String fileHash;
    @Enumerated(EnumType.STRING)
    private FileType type;
    @CreatedDate
    private LocalDateTime createdAt;
    @ManyToOne
    private User user;

    public File(){
    }


    public enum FileStatus{
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED;
    }

    public enum FileType{
        DOC,
        IMAGE,
        VIDEO,
        OTHER;
    }

    public File(String name, String s3Key, String fileHash, FileType type, User user) {
        this.name = name;
        this.status = FileStatus.PENDING;
        this.s3Key = s3Key;
        this.fileHash = fileHash;
        this.type = type;
        this.user = user;
    }




    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
