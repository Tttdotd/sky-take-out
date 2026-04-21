package com.sky.service.impl;

import com.sky.service.CommonService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class CommonServiceImpl implements CommonService {

    @Value("${sky.minio.endpoint}")
    private String endpoint;

    @Value("${sky.minio.public-endpoint}")
    private String publicEndpoint;

    @Value("${sky.minio.access-key}")
    private String accessKey;

    @Value("${sky.minio.secret-key}")
    private String secretKey;

    @Value("${sky.minio.bucket-name}")
    private String bucketName;


    @Override
    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID() + extension;
        String datePath = LocalDate.now().toString().replace("-", "/");
        String objectName = datePath + "/" + newFileName;

        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("文件上传到MinIO失败", e);
        }

        String accessPath = publicEndpoint + "/" + bucketName + "/" + objectName;
        log.info("文件上传成功，访问路径: {}", accessPath);
        return accessPath;
    }
}
