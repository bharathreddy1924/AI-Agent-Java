package com.aiagent.model;

import java.util.Objects;

/**
 * Represents a tool/function call request emitted by the LLM.
 */
public class ToolCall {
    private String id;
    private String name;
    private String arguments;

    public ToolCall() {
    }

    public ToolCall(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolCall toolCall = (ToolCall) o;
        return Objects.equals(id, toolCall.id) &&
               Objects.equals(name, toolCall.name) &&
               Objects.equals(arguments, toolCall.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, arguments);
    }

    @Override
    public String toString() {
        return "ToolCall{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", arguments='" + arguments + '\'' +
                '}';
    }
}
