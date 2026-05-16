package com.homehn.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.homehn.backend.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public Map<String, Object> upload(MultipartFile file, String folder) {
        return upload(file, folder, detectResourceType(file));
    }

    public Map<String, Object> upload(MultipartFile file, String folder, String resourceType) {
        try {
            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", folder, "resource_type", resourceType)
            );
        } catch (Exception e) {
            log.error("Cloudinary upload failed. folder={}, resourceType={}, fileName={}, size={}",
                    folder, resourceType, file.getOriginalFilename(), file.getSize(), e);
            throw new AppException("Upload tep len Cloudinary that bai: " + e.getMessage());
        }
    }

    public void delete(String publicId) {
        delete(publicId, "image");
    }

    public void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (Exception e) {
            log.error("Cloudinary delete failed. publicId={}, resourceType={}", publicId, resourceType, e);
            throw new AppException("Xoa tep tren Cloudinary that bai: " + e.getMessage());
        }
    }

    private String detectResourceType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/") ? "video" : "image";
    }
}
