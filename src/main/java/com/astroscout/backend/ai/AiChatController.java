package com.astroscout.backend.ai;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * Natural language astronomy Q&A. Uses RAG-style context (celestial catalog)
     * and LLM to answer. PRD 2.4.1. Allowed for both Guest and authenticated users.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String answer = aiChatService.chat(request.message());
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
