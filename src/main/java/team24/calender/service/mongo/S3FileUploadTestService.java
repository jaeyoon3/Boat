package team24.calender.service.mongo;


import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class S3FileUploadTestService {

    private final AmazonS3Client amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    private String PreFileName;
    private String dir = "/image";
    private String Userdir = "/UserImage";
    private String defaultUrl = "uri";

    public String uploadFile(MultipartFile file) throws IOException {

        String bucketDir = bucketName + dir;
        String dirUrl = defaultUrl + dir + "/";
        String fileName = generateFileName(file);

        amazonS3.putObject(bucketDir, fileName, file.getInputStream(), getObjectMetadata(file));
        return dirUrl + fileName;

    }

    private ObjectMetadata getObjectMetadata(MultipartFile file) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());
        return objectMetadata;
    }

    private String generateFileName(MultipartFile file) {
        return UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
    }
    public void deleteFile(String fileName) throws IOException {
        try {
            amazonS3.deleteObject(bucketName, fileName);
        } catch (SdkClientException e) {
            throw new IOException("Error deleting file from S3", e);
        }
    }
    public String uploadUserFile(MultipartFile file) throws IOException {

        String bucketDir = bucketName + Userdir;
        String dirUrl = defaultUrl + Userdir + "/";
        String fileName = generateFileName(file);

        amazonS3.putObject(bucketDir, fileName, file.getInputStream(), getObjectMetadata(file));
        return dirUrl + fileName;

    }


}
