package com.aiagent.llm;

import com.aiagent.model.ChatResponse;
import com.aiagent.model.ToolCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenAiCompatibleLlmService Response & JSON Parsing Tests")
class OpenAiCompatibleLlmServiceTest {

    private OpenAiCompatibleLlmService llmService;

    @BeforeEach
    void setUp() {
        llmService = new OpenAiCompatibleLlmService(
                "test-api-key",
                "https://api.openai.com/v1",
                "gpt-4o-mini",
                Duration.ofSeconds(10),
                HttpClient.newHttpClient(),
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("Should correctly parse OpenAI response with function/tool calls")
    void testParseResponseWithToolCalls() throws Exception {
        String mockResponseJson = """
                {
                    "id": "chatcmpl-test12345",
                    "object": "chat.completion",
                    "created": 1720000000,
                    "model": "gpt-4o-mini",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": null,
                                "tool_calls": [
                                    {
                                        "id": "call_abc123",
                                        "type": "function",
                                        "function": {
                                            "name": "calculator",
                                            "arguments": "{\\"operation\\":\\"multiply\\",\\"a\\":125,\\"b\\":48}"
                                        }
                                    }
                                ]
                            },
                            "finish_reason": "tool_calls"
                        }
                    ]
                }
                """;

        ChatResponse response = llmService.parseChatCompletionResponse(mockResponseJson);

        assertNotNull(response);
        assertTrue(response.hasToolCalls());
        assertEquals("tool_calls", response.getFinishReason());
        assertEquals(1, response.getToolCalls().size());

        ToolCall toolCall = response.getToolCalls().get(0);
        assertEquals("call_abc123", toolCall.getId());
        assertEquals("calculator", toolCall.getName());
        assertEquals("{\"operation\":\"multiply\",\"a\":125,\"b\":48}", toolCall.getArguments());
    }

    @Test
    @DisplayName("Should correctly parse regular text response from LLM")
    void testParseResponseWithContentOnly() throws Exception {
        String mockResponseJson = """
                {
                    "id": "chatcmpl-test67890",
                    "object": "chat.completion",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "The result is 6000."
                            },
                            "finish_reason": "stop"
                        }
                    ]
                }
                """;

        ChatResponse response = llmService.parseChatCompletionResponse(mockResponseJson);

        assertNotNull(response);
        assertFalse(response.hasToolCalls());
        assertEquals("The result is 6000.", response.getContent());
    }

    @Test
    @DisplayName("Should handle empty choices array gracefully")
    void testParseEmptyChoices() throws Exception {
        String emptyJson = "{\"choices\": []}";
        ChatResponse response = llmService.parseChatCompletionResponse(emptyJson);

        assertNotNull(response);
        assertTrue(response.getContent().contains("empty choices"));
    }
}
