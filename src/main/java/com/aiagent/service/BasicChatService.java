package com.aiagent.service;

import com.aiagent.llm.LlmService;
import com.aiagent.model.ChatResponse;
import com.aiagent.model.Message;

import java.util.Collections;
import java.util.List;

/**
 * Service providing direct prompt-and-response AI chat without tool calling.
 * Fulfills Feature 1: Basic AI Chat.
 */
public class BasicChatService {

    private final LlmService llmService;

    public BasicChatService(LlmService llmService) {
        if (llmService == null) {
            throw new IllegalArgumentException("LlmService cannot be null");
        }
        this.llmService = llmService;
    }

    /**
     * Sends a user's prompt to the LLM and receives a direct response.
     *
     * @param userPrompt The prompt text from the user.
     * @return The LLM's response text.
     */
    public String chat(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return "Please provide a valid, non-empty prompt.";
        }

        List<Message> messages = List.of(
                Message.system("You are a helpful and intelligent AI assistant. Provide concise and accurate answers."),
                Message.user(userPrompt.trim())
        );

        ChatResponse response = llmService.chat(messages, Collections.emptyList());
        return response.getContent() != null ? response.getContent() : "[Empty response received]";
    }
}
