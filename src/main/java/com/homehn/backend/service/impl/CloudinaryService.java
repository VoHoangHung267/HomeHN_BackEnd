package com.homehn.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.homehn.backend.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
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
        } catch (IOException e) {
            throw new AppException("Upload tệp thất bại: " + e.getMessage());
        }
    }

    public void delete(String publicId) {
        delete(publicId, "image");
    }

    public void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (IOException e) {
            throw new AppException("Xoá tệp thất bại: " + e.getMessage());
        }
    }

    private String detectResourceType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/") ? "video" : "image";
    }
}
