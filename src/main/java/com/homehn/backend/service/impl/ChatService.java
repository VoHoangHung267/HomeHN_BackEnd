package com.homehn.backend.service.impl;

import com.homehn.backend.dto.response.ChatRoomResponse;
import com.homehn.backend.dto.response.MessageResponse;
import com.homehn.backend.dto.response.RoomResponse;
import com.homehn.backend.entity.ChatRoomEntity;
import com.homehn.backend.entity.MessageEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.RoomImageEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.ChatRoomRepository;
import com.homehn.backend.repository.MessageRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepo;
    private final MessageRepository messageRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messaging;

    public ChatRoomResponse getOrCreate(Long roomId, Long seekerId) {
        return chatRoomRepo.findByRoom_IdAndSeeker_Id(roomId, seekerId)
                .map(this::toChatRoomResponse)
                .orElseGet(() -> {
                    RoomEntity room = roomRepo.findById(roomId)
                            .orElseThrow(() -> new AppException("Phòng không tồn tại", 404));
                    if (room.getStatus() != RoomEntity.RoomStatus.ACTIVE) {
                        throw new AppException("Chỉ có thể nhắn tin với phòng đang hiển thị");
                    }
                    if (room.getLandlord().getId().equals(seekerId)) {
                        throw new AppException("Bạn không thể tự nhắn tin với phòng của mình");
                    }
                    UserEntity seeker = userRepo.findById(seekerId).orElseThrow();
                    ChatRoomEntity cr = ChatRoomEntity.builder()
                            .room(room).seeker(seeker).landlord(room.getLandlord())
                            .build();
                    return toChatRoomResponse(chatRoomRepo.save(cr));
                });
    }

    public void markAsRead(Long chatRoomId, Long currentUserId) {
        requireParticipant(chatRoomId, currentUserId);
        messageRepo.markAllAsRead(chatRoomId, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long chatRoomId, Long currentUserId) {
        requireParticipant(chatRoomId, currentUserId);
        return messageRepo.findByChatRoom_IdOrderBySentAtAsc(chatRoomId)
                .stream().map(this::toMessageResponse).toList();
    }

    public MessageResponse sendMessage(Long chatRoomId, Long senderId, String content) {
        ChatRoomEntity chatRoom = requireParticipant(chatRoomId, senderId);
        UserEntity sender = userRepo.findById(senderId).orElseThrow();

        MessageEntity msg = messageRepo.save(MessageEntity.builder()
                .chatRoom(chatRoom).sender(sender).content(content).build());

        chatRoom.setLastMessage(content);
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepo.save(chatRoom);

        MessageResponse resp = toMessageResponse(msg);
        Long recipientId = chatRoom.getSeeker().getId().equals(senderId)
                ? chatRoom.getLandlord().getId()
                : chatRoom.getSeeker().getId();
        messaging.convertAndSendToUser(recipientId.toString(), "/queue/messages", resp);

        return resp;
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(Long userId, UserEntity.Role role) {
        List<ChatRoomEntity> rooms = switch (role) {
            case LANDLORD -> chatRoomRepo.findByLandlord_Id(userId);
            default -> chatRoomRepo.findBySeeker_Id(userId);
        };
        return rooms.stream().map(cr -> {
            ChatRoomResponse resp = toChatRoomResponse(cr);
            resp.setUnreadCount(messageRepo.countUnread(cr.getId(), userId));
            return resp;
        }).toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomContext(Long chatRoomId, Long currentUserId) {
        ChatRoomEntity chatRoom = requireParticipant(chatRoomId, currentUserId);
        RoomEntity room = roomRepo.findByIdWithImages(chatRoom.getRoom().getId())
                .orElseThrow(() -> new AppException("Phòng không tồn tại", 404));

        RoomResponse response = RoomResponse.from(room);
        response.setFavorited(false);
        return response;
    }

    private MessageResponse toMessageResponse(MessageEntity m) {
        return MessageResponse.builder()
                .id(m.getId())
                .chatRoomId(m.getChatRoom().getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFullName())
                .senderAvatar(m.getSender().getAvatarUrl())
                .content(m.getContent())
                .isRead(m.isRead())
                .sentAt(m.getSentAt())
                .build();
    }

    private ChatRoomResponse toChatRoomResponse(ChatRoomEntity cr) {
        String primaryImg = cr.getRoom().getImages().stream()
                .filter(RoomImageEntity::isPrimary)
                .map(RoomImageEntity::getImageUrl)
                .findFirst().orElse(null);

        return ChatRoomResponse.builder()
                .id(cr.getId())
                .roomId(cr.getRoom().getId())
                .roomTitle(cr.getRoom().getTitle())
                .roomPrimaryImage(primaryImg)
                .seekerId(cr.getSeeker().getId())
                .seekerName(cr.getSeeker().getFullName())
                .landlordId(cr.getLandlord().getId())
                .landlordName(cr.getLandlord().getFullName())
                .lastMessage(cr.getLastMessage())
                .lastMessageAt(cr.getLastMessageAt())
                .build();
    }

    private ChatRoomEntity requireParticipant(Long chatRoomId, Long userId) {
        ChatRoomEntity chatRoom = chatRoomRepo.findById(chatRoomId)
                .orElseThrow(() -> new AppException("Chat room không tồn tại", 404));
        boolean participant = chatRoom.getSeeker().getId().equals(userId)
                || chatRoom.getLandlord().getId().equals(userId);
        if (!participant) {
            throw new AppException("Bạn không có quyền truy cập cuộc trò chuyện này", 403);
        }
        return chatRoom;
    }
}
