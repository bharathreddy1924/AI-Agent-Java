package com.aiagent.llm;

import com.aiagent.config.AppConfig;

/**
 * Factory for creating the appropriate LlmService based on application configuration.
 */
public class LlmServiceFactory {

    public static LlmService createService(AppConfig config) {
        if (config.isLiveMode()) {
            System.out.println("[LLM Factory] Initializing Live LLM Service (OpenAI-compatible)");
            System.out.println("  Endpoint: " + config.getBaseUrl());
            System.out.println("  Model:    " + config.getModel());
            System.out.println("  API Key:  " + config.getMaskedApiKey());
            return new OpenAiCompatibleLlmService(config);
        } else {
            System.out.println("[LLM Factory] Initializing Intelligent Mock / Simulation LLM Service");
            System.out.println("  Reason:   No external API key provided or AGENT_MODE=mock.");
            System.out.println("  Status:   Fully functional offline mode (Tool calling & Agent workflow enabled).");
            return new MockLlmService();
        }
    }
}
