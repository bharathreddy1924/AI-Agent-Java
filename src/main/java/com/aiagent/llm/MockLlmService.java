package com.aiagent.llm;

import com.aiagent.model.ChatResponse;
import com.aiagent.model.Message;
import com.aiagent.model.Role;
import com.aiagent.model.ToolCall;
import com.aiagent.tool.Tool;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Mock/Simulated LLM Service.
 * Implements full conversational and tool-calling semantics without requiring an external API key.
 * Allows 100% deterministic local testing, offline demos, and grading.
 */
public class MockLlmService implements LlmService {

    private static final Pattern MULTIPLY_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:multiplied by|times|\\*|x)\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern ADD_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:plus|add|added to|\\+)\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern SUBTRACT_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:minus|subtract|subtracted by|-)\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern DIVIDE_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:divided by|over|/)\\s*(\\d+(?:\\.\\d+)?)");

    private static final Pattern WEATHER_PATTERN = Pattern.compile("(?i)(?:weather in|weather for|temperature in|forecast for|weather of)\\s+([a-zA-Z\\s]+)");

    @Override
    public ChatResponse chat(List<Message> messages, List<Tool> availableTools) {
        if (messages == null || messages.isEmpty()) {
            return ChatResponse.text("Hello! How can I assist you today?");
        }

        Message lastMessage = messages.get(messages.size() - 1);

        // Case 1: The previous step was a tool execution result (Role.TOOL)
        if (lastMessage.getRole() == Role.TOOL) {
            return synthesizeFinalResponse(messages, lastMessage);
        }

        // Case 2: User prompt - determine if a tool is required
        String userQuery = lastMessage.getContent() != null ? lastMessage.getContent().trim() : "";

        // Check if tools are enabled
        boolean hasTools = availableTools != null && !availableTools.isEmpty();

        if (hasTools) {
            // Check for math calculation intent
            ToolCall mathCall = detectMathToolCall(userQuery);
            if (mathCall != null && hasTool(availableTools, "calculator")) {
                return ChatResponse.toolCalls(List.of(mathCall));
            }

            // Check for weather intent
            ToolCall weatherCall = detectWeatherToolCall(userQuery);
            if (weatherCall != null && hasTool(availableTools, "get_weather")) {
                return ChatResponse.toolCalls(List.of(weatherCall));
            }
        }

        // Case 3: Direct conversational answer without tools
        return generateDirectResponse(userQuery);
    }

    private boolean hasTool(List<Tool> tools, String toolName) {
        return tools.stream().anyMatch(t -> t.getName().equalsIgnoreCase(toolName));
    }

    private ToolCall detectMathToolCall(String text) {
        String callId = "call_" + UUID.randomUUID().toString().substring(0, 8);

        Matcher mMult = MULTIPLY_PATTERN.matcher(text);
        if (mMult.find()) {
            double a = Double.parseDouble(mMult.group(1));
            double b = Double.parseDouble(mMult.group(2));
            return new ToolCall(callId, "calculator", String.format("{\"operation\":\"multiply\",\"a\":%s,\"b\":%s}", formatNum(a), formatNum(b)));
        }

        Matcher mDiv = DIVIDE_PATTERN.matcher(text);
        if (mDiv.find()) {
            double a = Double.parseDouble(mDiv.group(1));
            double b = Double.parseDouble(mDiv.group(2));
            return new ToolCall(callId, "calculator", String.format("{\"operation\":\"divide\",\"a\":%s,\"b\":%s}", formatNum(a), formatNum(b)));
        }

        Matcher mAdd = ADD_PATTERN.matcher(text);
        if (mAdd.find()) {
            double a = Double.parseDouble(mAdd.group(1));
            double b = Double.parseDouble(mAdd.group(2));
            return new ToolCall(callId, "calculator", String.format("{\"operation\":\"add\",\"a\":%s,\"b\":%s}", formatNum(a), formatNum(b)));
        }

        Matcher mSub = SUBTRACT_PATTERN.matcher(text);
        if (mSub.find()) {
            double a = Double.parseDouble(mSub.group(1));
            double b = Double.parseDouble(mSub.group(2));
            return new ToolCall(callId, "calculator", String.format("{\"operation\":\"subtract\",\"a\":%s,\"b\":%s}", formatNum(a), formatNum(b)));
        }

        return null;
    }

    private ToolCall detectWeatherToolCall(String text) {
        String callId = "call_" + UUID.randomUUID().toString().substring(0, 8);

        Matcher mWeather = WEATHER_PATTERN.matcher(text);
        if (mWeather.find()) {
            String city = mWeather.group(1).trim().replaceAll("[?.!]", "");
            return new ToolCall(callId, "get_weather", String.format("{\"city\":\"%s\"}", city));
        }

        if (text.toLowerCase().contains("weather") || text.toLowerCase().contains("temperature")) {
            // Default extract city word if available or London
            String[] words = text.split("\\s+");
            String city = "Tokyo";
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].equalsIgnoreCase("in") || words[i].equalsIgnoreCase("for")) {
                    city = words[i + 1].replaceAll("[^a-zA-Z]", "");
                    break;
                }
            }
            return new ToolCall(callId, "get_weather", String.format("{\"city\":\"%s\"}", city));
        }

        return null;
    }

    private ChatResponse synthesizeFinalResponse(List<Message> history, Message toolMsg) {
        String toolResultContent = toolMsg.getContent();

        // Find initial user prompt for context
        String originalPrompt = "";
        for (Message m : history) {
            if (m.getRole() == Role.USER) {
                originalPrompt = m.getContent();
            }
        }

        if (toolResultContent != null && toolResultContent.contains("Error")) {
            return ChatResponse.text("I attempted to perform the operation, but encountered an error: " + toolResultContent);
        }

        if (originalPrompt.toLowerCase().contains("multipl") || originalPrompt.toLowerCase().contains("divided")
                || originalPrompt.toLowerCase().contains("plus") || originalPrompt.toLowerCase().contains("minus")
                || originalPrompt.matches(".*\\d+.*[+*\\-/x].*")) {
            return ChatResponse.text("According to the calculation, the result is " + toolResultContent + ".");
        }

        if (originalPrompt.toLowerCase().contains("weather") || originalPrompt.toLowerCase().contains("temperature")) {
            return ChatResponse.text("Here is the requested weather data:\n" + toolResultContent);
        }

        return ChatResponse.text("The tool completed execution with the following result:\n" + toolResultContent);
    }

    private ChatResponse generateDirectResponse(String userQuery) {
        String lower = userQuery.toLowerCase();
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) {
            return ChatResponse.text("Hello! I am your AI-Powered Intelligent Workflow Agent. I can chat with you, solve arithmetic problems with my calculator tool, and look up live weather with my REST API tool.");
        }
        if (lower.contains("who are you") || lower.contains("what are you")) {
            return ChatResponse.text("I am an AI Intelligent Workflow Agent built in Java. I can reason about user requests, dynamically invoke tools, and synthesize answers.");
        }
        if (lower.contains("what can you do") || lower.contains("help")) {
            return ChatResponse.text("I can:\n" +
                    "1. Chat with you and answer questions.\n" +
                    "2. Perform arithmetic calculations using my Calculator Tool (e.g., 'What is 125 multiplied by 48?').\n" +
                    "3. Query live global weather using my REST API Tool (e.g., 'What is the weather in Tokyo?').");
        }

        return ChatResponse.text("I processed your request: \"" + userQuery + "\". No external tools were required to answer this inquiry.");
    }

    private String formatNum(double d) {
        if (d == (long) d) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
