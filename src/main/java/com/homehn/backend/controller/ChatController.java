package com.homehn.backend.controller;

import com.homehn.backend.dto.request.SendMessageRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ChatRoomResponse;
import com.homehn.backend.dto.response.MessageResponse;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
class ChatController {

    private final ChatService chatService;

    @PostMapping("/rooms/{roomId}/open")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> openChat(
            @PathVariable Long roomId, @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getOrCreate(roomId, user.getId())));
    }

    @GetMapping("/my-chats")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> myChats(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMyChatRooms(user.getId(), user.getRole())));
    }

    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable Long chatRoomId, @AuthenticationPrincipal UserPrincipal user
    ) {
        chatService.markAsRead(chatRoomId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessages(chatRoomId, user.getId())));
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest payload,
                            Principal principal) {
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) principal;
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();

        chatService.sendMessage(payload.getChatRoomId(), user.getId(), payload.getContent());
    }
}
