package com.aiagent.llm;

import com.aiagent.model.ChatResponse;
import com.aiagent.model.Message;
import com.aiagent.tool.Tool;

import java.util.List;

/**
 * Service interface for sending prompts to a Large Language Model (LLM).
 * Supports both standard chat completion and tool/function calling.
 */
public interface LlmService {

    /**
     * Sends the conversation messages and list of available tools to the LLM.
     *
     * @param messages Conversation history including system, user, assistant, and tool messages.
     * @param availableTools List of tools the LLM can decide to invoke. Can be empty or null for basic chat.
     * @return ChatResponse containing either a textual response or tool call requests.
     */
    ChatResponse chat(List<Message> messages, List<Tool> availableTools);
}
