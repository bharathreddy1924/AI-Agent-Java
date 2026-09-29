package com.aiagent;

import com.aiagent.agent.WorkflowAgent;
import com.aiagent.config.AppConfig;
import com.aiagent.llm.LlmService;
import com.aiagent.llm.LlmServiceFactory;
import com.aiagent.model.ToolResult;
import com.aiagent.service.BasicChatService;
import com.aiagent.tool.CalculatorTool;
import com.aiagent.tool.ToolRegistry;
import com.aiagent.tool.WeatherRestTool;

import java.util.Scanner;

/**
 * Main entry point for the AI-Powered Intelligent Workflow Agent application.
 * Provides interactive console interface and automated demonstrations.
 */
public class App {

    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        ToolRegistry registry = new ToolRegistry();

        // Register tools
        CalculatorTool calculatorTool = new CalculatorTool();
        WeatherRestTool weatherTool = new WeatherRestTool();
        registry.registerTool(calculatorTool);
        registry.registerTool(weatherTool);

        // Initialize services
        LlmService llmService = LlmServiceFactory.createService(config);
        BasicChatService basicChatService = new BasicChatService(llmService);
        WorkflowAgent agent = new WorkflowAgent(llmService, registry, true);

        // If command-line argument "demo" is passed, run non-interactive automated showcase
        if (args.length > 0 && "demo".equalsIgnoreCase(args[0])) {
            runAutomatedShowcase(basicChatService, calculatorTool, weatherTool, agent);
            return;
        }

        printBanner(config, registry);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("Select an option (1-8): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.println("\n--- [Feature 1: Basic AI Chat] ---");
                    System.out.print("Enter your prompt for the LLM: ");
                    String prompt = scanner.nextLine().trim();
                    if (!prompt.isEmpty()) {
                        System.out.println("\n[LLM Response]:");
                        System.out.println(basicChatService.chat(prompt));
                    }
                }
                case "2" -> {
                    System.out.println("\n--- [Feature 3: Calculator Tool Demo] ---");
                    System.out.println("1) Valid: 125 * 48");
                    ToolResult r1 = calculatorTool.execute("{\"operation\":\"multiply\",\"a\":125,\"b\":48}");
                    System.out.println("   Result: " + r1.getContent() + " (Error: " + r1.isError() + ")");

                    System.out.println("2) Division by zero: 50 / 0");
                    ToolResult r2 = calculatorTool.execute("{\"operation\":\"divide\",\"a\":50,\"b\":0}");
                    System.out.println("   Result: " + r2.getContent() + " (Error: " + r2.isError() + ")");

                    System.out.println("3) Invalid argument input:");
                    ToolResult r3 = calculatorTool.execute("{\"operation\":\"unknown\",\"a\":\"abc\",\"b\":10}");
                    System.out.println("   Result: " + r3.getContent() + " (Error: " + r3.isError() + ")");
                }
                case "3" -> {
                    System.out.println("\n--- [Feature 4: Public REST API Tool (Open-Meteo)] ---");
                    System.out.print("Enter city name (e.g., Tokyo, London, Paris, New York) [default: Tokyo]: ");
                    String city = scanner.nextLine().trim();
                    if (city.isEmpty()) city = "Tokyo";
                    System.out.println("Calling live Open-Meteo REST API for " + city + "...");
                    ToolResult wr = weatherTool.execute("{\"city\":\"" + city + "\"}");
                    System.out.println("Result:\n" + wr.getContent());
                }
                case "4" -> {
                    System.out.println("\n--- [Features 2 & 5: AI Agent Math Workflow] ---");
                    String query = "What is 125 multiplied by 48?";
                    agent.execute(query);
                }
                case "5" -> {
                    System.out.println("\n--- [Features 2 & 5: AI Agent REST API Workflow] ---");
                    String query = "What is the current weather in Tokyo?";
                    agent.execute(query);
                }
                case "6" -> {
                    System.out.println("\n--- [Features 2 & 5: AI Agent Direct Chat (No Tool Needed)] ---");
                    String query = "Who are you and what tools do you have access to?";
                    agent.execute(query);
                }
                case "7" -> {
                    System.out.println("\n--- [Feature 2: Interactive AI Agent Session] ---");
                    System.out.println("Type your prompt for the Agent (or 'back' to return to menu):");
                    while (true) {
                        System.out.print("\nYou: ");
                        String userPrompt = scanner.nextLine().trim();
                        if (userPrompt.equalsIgnoreCase("back") || userPrompt.equalsIgnoreCase("exit")) {
                            break;
                        }
                        if (!userPrompt.isEmpty()) {
                            agent.execute(userPrompt);
                        }
                    }
                }
                case "8" -> {
                    System.out.println("Exiting AI-Powered Intelligent Workflow Agent. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please choose an option between 1 and 8.");
            }
        }
    }

    public static void runAutomatedShowcase(BasicChatService basicChatService,
                                            CalculatorTool calculatorTool,
                                            WeatherRestTool weatherTool,
                                            WorkflowAgent agent) {
        System.out.println("\n========================================================");
        System.out.println("   RUNNING AUTOMATED SHOWCASE OF ALL PROJECT FEATURES   ");
        System.out.println("========================================================");

        // 1. Basic Chat
        System.out.println("\n[1] Testing Feature 1: Basic AI Chat");
        String chatRes = basicChatService.chat("Explain what an AI Agent is in one sentence.");
        System.out.println("Response: " + chatRes);

        // 2. Calculator Tool
        System.out.println("\n[2] Testing Feature 3: Calculator Tool (125 * 48)");
        ToolResult calcRes = calculatorTool.execute("{\"operation\":\"multiply\",\"a\":125,\"b\":48}");
        System.out.println("Calculator Result: " + calcRes.getContent() + " (IsError: " + calcRes.isError() + ")");

        // 3. Calculator Error Handling
        System.out.println("\n[3] Testing Feature 6: Calculator Division by Zero Handling");
        ToolResult divZero = calculatorTool.execute("{\"operation\":\"divide\",\"a\":100,\"b\":0}");
        System.out.println("Division by Zero Result: " + divZero.getContent() + " (IsError: " + divZero.isError() + ")");

        // 4. REST API Tool
        System.out.println("\n[4] Testing Feature 4: Public REST API Tool (Open-Meteo Weather)");
        ToolResult weatherRes = weatherTool.execute("{\"city\":\"London\"}");
        System.out.println("Weather Result:\n" + weatherRes.getContent());

        // 5. Agent Workflow with Calculator Tool
        System.out.println("\n[5] Testing Features 2 & 5: AI Agent Math Workflow");
        agent.execute("What is 125 multiplied by 48?");

        // 6. Agent Workflow with Weather Tool
        System.out.println("\n[6] Testing Features 2 & 5: AI Agent Weather REST Workflow");
        agent.execute("What is the current weather in Paris?");

        // 7. Agent Direct Conversation (No Tool)
        System.out.println("\n[7] Testing Feature 2: AI Agent Direct Conversation (No Tool Needed)");
        agent.execute("Hello! Who are you?");

        System.out.println("\n========================================================");
        System.out.println("      AUTOMATED SHOWCASE COMPLETED SUCCESSFULLY!        ");
        System.out.println("========================================================\n");
    }

    private static void printBanner(AppConfig config, ToolRegistry registry) {
        System.out.println("""
                ************************************************************
                *        AI-POWERED INTELLIGENT WORKFLOW AGENT             *
                *                Java 17 + Maven + ReAct                   *
                ************************************************************
                """);
        System.out.println("Configuration Status:");
        System.out.println("  Mode:        " + (config.isLiveMode() ? "LIVE API" : "OFFLINE SIMULATION / MOCK"));
        System.out.println("  Base URL:    " + config.getBaseUrl());
        System.out.println("  Model:       " + config.getModel());
        System.out.println("  API Key:     " + config.getMaskedApiKey());
        System.out.println("  Timeout:     " + config.getTimeoutSeconds() + "s");
        System.out.println("Registered Tools: " + registry.getToolNames());
        System.out.println();
    }

    private static void printMenu() {
        System.out.println("""
                ------------------------------------------------------------
                MAIN MENU:
                1. Basic AI Chat (Direct LLM Prompt)
                2. Calculator Tool (125 * 48, Division by Zero, Invalid Input)
                3. REST API Tool (Live Weather Lookup)
                4. Agent Workflow - Math Calculation ("What is 125 multiplied by 48?")
                5. Agent Workflow - Weather REST API ("What is the weather in Tokyo?")
                6. Agent Workflow - Direct Question ("Who are you?")
                7. Interactive Agent Prompt (Chat freely with the Agent)
                8. Exit
                ------------------------------------------------------------""");
    }
}
