package com.homehn.backend.service.impl;

import com.homehn.backend.entity.NotificationEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void notifyUser(UserEntity user, String type, String title, String message, Long relatedId) {
        notificationRepository.save(NotificationEntity.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .build());
    }
}
