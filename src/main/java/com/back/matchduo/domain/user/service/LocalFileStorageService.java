package com.back.matchduo.domain.user.service;

import com.back.matchduo.global.exeption.CustomErrorCode;
import com.back.matchduo.global.exeption.CustomException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Profile({"dev", "test"})
public class LocalFileStorageService implements FileStorageService {

    private static final Path UPLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "uploads", "profile");

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(CustomErrorCode.INVALID_FILE);
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = UPLOAD_DIR.resolve(fileName);

        try {
            Files.createDirectories(UPLOAD_DIR);
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            throw new CustomException(CustomErrorCode.FILE_UPLOAD_FAILED);
        }

        return "/uploads/profile/" + fileName;
    }

    @Override
    public void delete(String imageUrl) {
        if (imageUrl == null) return;

        Path path = Paths.get("." + imageUrl); // "/uploads/..." → 로컬 경로
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new CustomException(CustomErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}