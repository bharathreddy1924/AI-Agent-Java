package com.aiagent.model;

/**
 * Encapsulates the execution result of a tool.
 */
public class ToolResult {
    private final String toolCallId;
    private final String toolName;
    private final String content;
    private final boolean error;

    public ToolResult(String toolCallId, String toolName, String content, boolean error) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.content = content;
        this.error = error;
    }

    public static ToolResult success(String toolCallId, String toolName, String content) {
        return new ToolResult(toolCallId, toolName, content, false);
    }

    public static ToolResult success(String toolName, String content) {
        return new ToolResult(null, toolName, content, false);
    }

    public static ToolResult error(String toolCallId, String toolName, String errorMessage) {
        return new ToolResult(toolCallId, toolName, errorMessage, true);
    }

    public static ToolResult error(String toolName, String errorMessage) {
        return new ToolResult(null, toolName, errorMessage, true);
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getContent() {
        return content;
    }

    public boolean isError() {
        return error;
    }

    @Override
    public String toString() {
        return "ToolResult{" +
                "toolCallId='" + toolCallId + '\'' +
                ", toolName='" + toolName + '\'' +
                ", content='" + content + '\'' +
                ", error=" + error +
                '}';
    }
}
