package com.aiagent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the response from an LLM service call.
 */
public class ChatResponse {
    private String content;
    private List<ToolCall> toolCalls;
    private String finishReason;

    public ChatResponse() {
        this.toolCalls = new ArrayList<>();
    }

    public ChatResponse(String content) {
        this.content = content;
        this.toolCalls = new ArrayList<>();
        this.finishReason = "stop";
    }

    public ChatResponse(String content, List<ToolCall> toolCalls, String finishReason) {
        this.content = content;
        this.toolCalls = toolCalls != null ? toolCalls : new ArrayList<>();
        this.finishReason = finishReason;
    }

    public static ChatResponse text(String content) {
        return new ChatResponse(content);
    }

    public static ChatResponse toolCalls(List<ToolCall> toolCalls) {
        return new ChatResponse(null, toolCalls, "tool_calls");
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "content='" + content + '\'' +
                ", toolCalls=" + toolCalls +
                ", finishReason='" + finishReason + '\'' +
                '}';
    }
}
