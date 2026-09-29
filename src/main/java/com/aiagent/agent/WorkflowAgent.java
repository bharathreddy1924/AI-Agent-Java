package com.aiagent.agent;

import com.aiagent.llm.LlmService;
import com.aiagent.model.*;
import com.aiagent.tool.Tool;
import com.aiagent.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Intelligent Workflow Agent.
 * Orchestrates the full Agentic ReAct / Tool-Calling Loop:
 * 1. Receives user query.
 * 2. Consults LLM with available tools.
 * 3. Decides whether a tool is required.
 * 4. Dispatches tool execution.
 * 5. Returns tool result back to the LLM.
 * 6. Synthesizes and returns the final response.
 */
public class WorkflowAgent {

    private static final int MAX_ITERATIONS = 5;

    private final LlmService llmService;
    private final ToolRegistry toolRegistry;
    private final boolean verboseLogging;

    public WorkflowAgent(LlmService llmService, ToolRegistry toolRegistry) {
        this(llmService, toolRegistry, true);
    }

    public WorkflowAgent(LlmService llmService, ToolRegistry toolRegistry, boolean verboseLogging) {
        if (llmService == null) {
            throw new IllegalArgumentException("LlmService cannot be null");
        }
        if (toolRegistry == null) {
            throw new IllegalArgumentException("ToolRegistry cannot be null");
        }
        this.llmService = llmService;
        this.toolRegistry = toolRegistry;
        this.verboseLogging = verboseLogging;
    }

    /**
     * Executes the workflow agent on a user query.
     *
     * @param userPrompt The request from the user.
     * @return Final response text synthesized by the agent.
     */
    public String execute(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return "Error: User prompt cannot be empty.";
        }

        log("\n========================================================");
        log("[Agent] Received user query: \"" + userPrompt.trim() + "\"");

        List<Message> history = new ArrayList<>();
        history.add(Message.system(
                "You are an intelligent workflow agent. " +
                "You have access to specialized tools. When answering questions that require calculation " +
                "or real-time weather information, call the appropriate tool. " +
                "Once you receive the tool's result, synthesize a clear, helpful final answer."
        ));
        history.add(Message.user(userPrompt.trim()));

        int iteration = 0;
        while (iteration < MAX_ITERATIONS) {
            iteration++;
            log("[Agent Step " + iteration + "] Consulting LLM with " + toolRegistry.getAllTools().size() + " available tool(s)...");

            ChatResponse response = llmService.chat(history, toolRegistry.getAllTools());

            if (response == null) {
                return "Error: Received null response from LLM service.";
            }

            // Case A: No tool calls needed -> Direct response or final synthesis completed
            if (!response.hasToolCalls()) {
                String finalContent = response.getContent() != null ? response.getContent() : "[Empty response]";
                log("[Agent Decision] No further tools needed. Final answer reached.");
                log("[Agent Final Answer]:\n" + finalContent);
                log("========================================================\n");
                return finalContent;
            }

            // Case B: Tool call(s) requested by LLM
            log("[Agent Decision] Tool execution required!");
            List<ToolCall> toolCalls = response.getToolCalls();
            log("  Requested " + toolCalls.size() + " tool call(s).");

            // Add the assistant's tool-call message to history
            history.add(Message.assistantWithTools(response.getContent(), toolCalls));

            // Execute each requested tool
            for (ToolCall toolCall : toolCalls) {
                String toolName = toolCall.getName();
                String args = toolCall.getArguments();
                String callId = toolCall.getId();

                log("  -> Invoking tool: '" + toolName + "'");
                log("     Arguments: " + args);

                Tool tool = toolRegistry.getTool(toolName);
                ToolResult result;

                if (tool == null) {
                    String errMsg = "Error: Tool '" + toolName + "' is not registered.";
                    log("     [Tool Error]: " + errMsg);
                    result = ToolResult.error(callId, toolName, errMsg);
                } else {
                    try {
                        result = tool.execute(callId, args);
                        log("     [Tool Result] Success=" + (!result.isError()) + ":\n" + indent(result.getContent(), 7));
                    } catch (Exception e) {
                        String errMsg = "Exception during tool execution: " + e.getMessage();
                        log("     [Tool Exception]: " + errMsg);
                        result = ToolResult.error(callId, toolName, errMsg);
                    }
                }

                // Append tool result message to conversation history
                history.add(Message.tool(result.getToolCallId(), result.getContent()));
            }

            log("[Agent] Feeding tool results back to LLM for final synthesis...");
        }

        return "Error: Agent reached maximum iterations (" + MAX_ITERATIONS + ") without reaching a final answer.";
    }

    private void log(String message) {
        if (verboseLogging) {
            System.out.println(message);
        }
    }

    private String indent(String text, int spaces) {
        if (text == null) return "";
        String prefix = " ".repeat(spaces);
        return prefix + text.replace("\n", "\n" + prefix);
    }
}
