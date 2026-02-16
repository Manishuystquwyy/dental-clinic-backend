package com.gayatri.dentalclinic.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeDentistPicture(Long dentistId, MultipartFile file);
}
