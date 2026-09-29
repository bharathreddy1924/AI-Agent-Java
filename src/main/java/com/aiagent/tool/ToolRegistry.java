package com.aiagent.tool;

import java.util.*;

/**
 * Registry holding all available tools accessible by the AI Agent.
 * Generates OpenAI-compatible function calling schemas for LLM requests.
 */
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry() {
    }

    public void registerTool(Tool tool) {
        if (tool != null) {
            tools.put(tool.getName(), tool);
        }
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }

    /**
     * Converts registered tools into OpenAI-compatible tool specifications.
     * Format:
     * [
     *   {
     *     "type": "function",
     *     "function": {
     *       "name": "...",
     *       "description": "...",
     *       "parameters": { ... }
     *     }
     *   }
     * ]
     */
    public List<Map<String, Object>> getToolSpecifications() {
        List<Map<String, Object>> specs = new ArrayList<>();
        for (Tool tool : tools.values()) {
            Map<String, Object> spec = new LinkedHashMap<>();
            spec.put("type", "function");

            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());
            function.put("parameters", tool.getParameterSchema());

            spec.put("function", function);
            specs.add(spec);
        }
        return specs;
    }
}
