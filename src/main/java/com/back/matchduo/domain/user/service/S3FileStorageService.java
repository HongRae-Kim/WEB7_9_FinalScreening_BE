package com.back.matchduo.domain.user.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.back.matchduo.global.exeption.CustomErrorCode;
import com.back.matchduo.global.exeption.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(CustomErrorCode.INVALID_REQUEST);
        }

        String fileName = "profile/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try {
            amazonS3.putObject(
                    bucket,
                    fileName,
                    file.getInputStream(),
                    metadata
            );
        } catch (IOException e) {
            throw new CustomException(CustomErrorCode.FILE_UPLOAD_FAILED);
        }

        return amazonS3.getUrl(bucket, fileName).toString();
    }

    @Override
    public void delete(String imageUrl) {
        if (imageUrl == null) return;

        // https://bucket.s3.region.amazonaws.com/profile/xxx.png
        String key = imageUrl.substring(imageUrl.indexOf("profile/"));

        amazonS3.deleteObject(bucket, key);
    }
}