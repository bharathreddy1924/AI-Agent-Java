package com.aiagent.agent;

import com.aiagent.llm.MockLlmService;
import com.aiagent.tool.CalculatorTool;
import com.aiagent.tool.ToolRegistry;
import com.aiagent.tool.WeatherRestTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkflowAgent ReAct & Tool-Calling Tests")
class WorkflowAgentTest {

    private WorkflowAgent agent;
    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
        registry.registerTool(new CalculatorTool());
        registry.registerTool(new WeatherRestTool());

        MockLlmService mockLlm = new MockLlmService();
        // verboseLogging = false in unit tests to keep test output clean
        agent = new WorkflowAgent(mockLlm, registry, false);
    }

    @Test
    @DisplayName("Should process math request: 'What is 125 multiplied by 48?' using Calculator Tool")
    void testAgentMultiplicationWorkflow() {
        String query = "What is 125 multiplied by 48?";
        String response = agent.execute(query);

        assertNotNull(response);
        assertTrue(response.contains("6000"),
                "Final agent response should contain the calculation result 6000, but got: " + response);
    }

    @Test
    @DisplayName("Should process division request: 'What is 100 divided by 4?'")
    void testAgentDivisionWorkflow() {
        String query = "What is 100 divided by 4?";
        String response = agent.execute(query);

        assertNotNull(response);
        assertTrue(response.contains("25"),
                "Final agent response should contain 25, got: " + response);
    }

    @Test
    @DisplayName("Should answer conversational queries without triggering tools")
    void testAgentDirectChatWorkflow() {
        String query = "Who are you?";
        String response = agent.execute(query);

        assertNotNull(response);
        assertTrue(response.toLowerCase().contains("agent") || response.toLowerCase().contains("workflow"),
                "Response should be conversational, got: " + response);
    }

    @Test
    @DisplayName("Should handle division by zero within agent workflow gracefully")
    void testAgentDivisionByZeroWorkflow() {
        String query = "What is 50 divided by 0?";
        String response = agent.execute(query);

        assertNotNull(response);
        assertTrue(response.contains("Division by zero") || response.contains("error"),
                "Agent should report division by zero error gracefully, got: " + response);
    }

    @Test
    @DisplayName("Should reject empty or whitespace-only queries")
    void testEmptyPrompt() {
        String response = agent.execute("   ");
        assertNotNull(response);
        assertTrue(response.startsWith("Error:"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if initialized with null dependencies")
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new WorkflowAgent(null, registry));
        assertThrows(IllegalArgumentException.class, () -> new WorkflowAgent(new MockLlmService(), null));
    }
}
