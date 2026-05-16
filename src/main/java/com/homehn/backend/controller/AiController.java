package com.homehn.backend.controller;

import com.homehn.backend.dto.request.ChatAssistantRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ChatAssistantResponse;
import com.homehn.backend.service.impl.GeminiRoomCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
class AiController {

    private final GeminiRoomCopyService geminiRoomCopyService;

    @PostMapping("/assistant")
    public ResponseEntity<ApiResponse<ChatAssistantResponse>> assistant(
            @RequestBody ChatAssistantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã tạo câu trả lời từ trợ lý AI",
                geminiRoomCopyService.answerGeneralQuestion(request)
        ));
    }
}
