package com.aiagent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a chat message in the conversation.
 */
public class Message {
    private Role role;
    private String content;
    private List<ToolCall> toolCalls;
    private String toolCallId; // Used when role == TOOL

    public Message() {
        this.toolCalls = new ArrayList<>();
    }

    public Message(Role role, String content) {
        this.role = role;
        this.content = content;
        this.toolCalls = new ArrayList<>();
    }

    public Message(Role role, String content, List<ToolCall> toolCalls) {
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls != null ? toolCalls : new ArrayList<>();
    }

    public Message(Role role, String content, String toolCallId) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
        this.toolCalls = new ArrayList<>();
    }

    public static Message system(String content) {
        return new Message(Role.SYSTEM, content);
    }

    public static Message user(String content) {
        return new Message(Role.USER, content);
    }

    public static Message assistant(String content) {
        return new Message(Role.ASSISTANT, content);
    }

    public static Message assistantWithTools(String content, List<ToolCall> toolCalls) {
        return new Message(Role.ASSISTANT, content, toolCalls);
    }

    public static Message tool(String toolCallId, String content) {
        return new Message(Role.TOOL, content, toolCallId);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    @Override
    public String toString() {
        return "Message{" +
                "role=" + role +
                ", content='" + content + '\'' +
                ", toolCalls=" + toolCalls +
                ", toolCallId='" + toolCallId + '\'' +
                '}';
    }
}
