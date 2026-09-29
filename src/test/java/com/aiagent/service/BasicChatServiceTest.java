package com.aiagent.service;

import com.aiagent.llm.MockLlmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BasicChatService Tests")
class BasicChatServiceTest {

    private BasicChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new BasicChatService(new MockLlmService());
    }

    @Test
    @DisplayName("Should return valid response for conversational greeting")
    void testChatGreeting() {
        String response = chatService.chat("Hello assistant!");
        assertNotNull(response);
        assertFalse(response.isBlank());
        assertTrue(response.toLowerCase().contains("hello") || response.toLowerCase().contains("agent"));
    }

    @Test
    @DisplayName("Should handle empty or blank prompt gracefully")
    void testBlankPrompt() {
        String response = chatService.chat("   ");
        assertEquals("Please provide a valid, non-empty prompt.", response);

        String nullResponse = chatService.chat(null);
        assertEquals("Please provide a valid, non-empty prompt.", nullResponse);
    }
}
