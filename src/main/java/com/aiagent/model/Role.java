package com.aiagent.model;

/**
 * Enumeration of message roles in an LLM conversation.
 */
public enum Role {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Role fromValue(String text) {
        for (Role r : Role.values()) {
            if (r.value.equalsIgnoreCase(text)) {
                return r;
            }
        }
        return USER;
    }
}
