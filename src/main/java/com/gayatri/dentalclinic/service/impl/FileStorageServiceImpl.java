package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.config.FileStorageConfig;
import com.gayatri.dentalclinic.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FileStorageConfig fileStorageConfig;

    @Override
    public String storeDentistPicture(Long dentistId, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalName);
        String filename = "dentist-" + dentistId + "-" + UUID.randomUUID();
        if (ext != null && !ext.isBlank()) {
            filename += "." + ext;
        }

        Path uploadDir = fileStorageConfig.getUploadRoot().resolve("dentists");
        try {
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }

        return "/uploads/dentists/" + filename;
    }
}
