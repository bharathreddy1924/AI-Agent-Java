package com.aiagent.llm;

import com.aiagent.config.AppConfig;
import com.aiagent.model.ChatResponse;
import com.aiagent.model.Message;
import com.aiagent.model.Role;
import com.aiagent.model.ToolCall;
import com.aiagent.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Production-ready LLM service supporting OpenAI and OpenAI-compatible endpoints
 * (OpenAI, Groq, Ollama, OpenRouter, Gemini OpenAI-compatible).
 * Uses Java 11+ HttpClient and Jackson for lightweight, reliable execution.
 */
public class OpenAiCompatibleLlmService implements LlmService {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmService(AppConfig config) {
        this(config.getApiKey(),
             config.getBaseUrl(),
             config.getModel(),
             Duration.ofSeconds(config.getTimeoutSeconds()),
             HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds())).build(),
             new ObjectMapper());
    }

    public OpenAiCompatibleLlmService(String apiKey, String baseUrl, String model,
                                      Duration timeout, HttpClient httpClient, ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey : "";
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.model = (model != null && !model.trim().isEmpty()) ? model : "gpt-4o-mini";
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(30);
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "https://api.openai.com/v1";
        }
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    @Override
    public ChatResponse chat(List<Message> messages, List<Tool> availableTools) {
        try {
            Map<String, Object> requestPayload = new LinkedHashMap<>();
            requestPayload.put("model", model);

            // Format conversation history
            List<Map<String, Object>> messageList = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, Object> msgMap = new LinkedHashMap<>();
                msgMap.put("role", msg.getRole().getValue());

                if (msg.getContent() != null) {
                    msgMap.put("content", msg.getContent());
                } else {
                    msgMap.put("content", null);
                }

                if (msg.getRole() == Role.TOOL && msg.getToolCallId() != null) {
                    msgMap.put("tool_call_id", msg.getToolCallId());
                }

                if (msg.hasToolCalls()) {
                    List<Map<String, Object>> toolCallList = new ArrayList<>();
                    for (ToolCall tc : msg.getToolCalls()) {
                        Map<String, Object> tcMap = new LinkedHashMap<>();
                        tcMap.put("id", tc.getId());
                        tcMap.put("type", "function");

                        Map<String, Object> fnMap = new LinkedHashMap<>();
                        fnMap.put("name", tc.getName());
                        fnMap.put("arguments", tc.getArguments());
                        tcMap.put("function", fnMap);

                        toolCallList.add(tcMap);
                    }
                    msgMap.put("tool_calls", toolCallList);
                }

                messageList.add(msgMap);
            }
            requestPayload.put("messages", messageList);

            // Format tools if available
            if (availableTools != null && !availableTools.isEmpty()) {
                List<Map<String, Object>> toolSpecs = new ArrayList<>();
                for (Tool tool : availableTools) {
                    Map<String, Object> spec = new LinkedHashMap<>();
                    spec.put("type", "function");

                    Map<String, Object> fn = new LinkedHashMap<>();
                    fn.put("name", tool.getName());
                    fn.put("description", tool.getDescription());
                    fn.put("parameters", tool.getParameterSchema());
                    spec.put("function", fn);

                    toolSpecs.add(spec);
                }
                requestPayload.put("tools", toolSpecs);
                requestPayload.put("tool_choice", "auto");
            }

            String requestBodyJson = objectMapper.writeValueAsString(requestPayload);
            String endpoint = baseUrl + "/chat/completions";

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            if (!apiKey.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpRequest request = reqBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBodyJson)).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status != 200) {
                return handleHttpError(status, response.body());
            }

            return parseChatCompletionResponse(response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ChatResponse.text("Error: LLM request interrupted: " + e.getMessage());
        } catch (Exception e) {
            return ChatResponse.text("Error communicating with LLM API: " + e.getMessage());
        }
    }

    private ChatResponse handleHttpError(int statusCode, String responseBody) {
        String detail = responseBody;
        try {
            JsonNode errJson = objectMapper.readTree(responseBody);
            if (errJson.has("error") && errJson.get("error").has("message")) {
                detail = errJson.get("error").get("message").asText();
            }
        } catch (Exception ignored) {
        }

        return switch (statusCode) {
            case 401 -> ChatResponse.text("Error (HTTP 401 Unauthorized): Invalid or missing API key. Please check your OPENAI_API_KEY environment variable. Detail: " + detail);
            case 429 -> ChatResponse.text("Error (HTTP 429 Rate Limit Exceeded): You have hit the API rate limit or quota. Detail: " + detail);
            case 404 -> ChatResponse.text("Error (HTTP 404 Not Found): Endpoint or model not found. Check OPENAI_BASE_URL and OPENAI_MODEL. Detail: " + detail);
            default -> ChatResponse.text("Error (HTTP " + statusCode + ") from LLM API: " + detail);
        };
    }

    public ChatResponse parseChatCompletionResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return ChatResponse.text("Error: Received empty choices array from LLM response.");
        }

        JsonNode choice = choices.get(0);
        JsonNode messageNode = choice.path("message");
        String finishReason = choice.path("finish_reason").asText("stop");

        List<ToolCall> toolCalls = new ArrayList<>();
        if (messageNode.has("tool_calls") && messageNode.get("tool_calls").isArray()) {
            for (JsonNode tcNode : messageNode.get("tool_calls")) {
                String id = tcNode.path("id").asText("call_" + UUID.randomUUID().toString().substring(0, 8));
                String name = tcNode.path("function").path("name").asText();
                String args = tcNode.path("function").path("arguments").asText("{}");
                toolCalls.add(new ToolCall(id, name, args));
            }
        }

        String content = messageNode.path("content").asText(null);

        if (!toolCalls.isEmpty()) {
            return new ChatResponse(content, toolCalls, finishReason);
        } else {
            return ChatResponse.text(content != null ? content : "");
        }
    }
}
