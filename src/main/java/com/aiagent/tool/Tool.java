package com.aiagent.tool;

import com.aiagent.model.ToolResult;

import java.util.Map;

/**
 * Interface representing a tool that can be invoked by the AI agent.
 */
public interface Tool {

    /**
     * Unique identifier for the tool (e.g., "calculator", "get_weather").
     */
    String getName();

    /**
     * Clear description explaining what the tool does and when the LLM should invoke it.
     */
    String getDescription();

    /**
     * JSON Schema representation of the tool parameters expected by the LLM.
     */
    Map<String, Object> getParameterSchema();

    /**
     * Executes the tool with the provided arguments in JSON format.
     *
     * @param argumentsJson JSON string representing tool arguments.
     * @return Execution result wrapped in a ToolResult.
     */
    ToolResult execute(String argumentsJson);

    /**
     * Executes the tool with a specific toolCallId.
     */
    default ToolResult execute(String toolCallId, String argumentsJson) {
        ToolResult res = execute(argumentsJson);
        return new ToolResult(toolCallId, getName(), res.getContent(), res.isError());
    }
}
