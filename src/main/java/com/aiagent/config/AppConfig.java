package com.aiagent.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Application configuration manager.
 * Reads configurations from System Environment Variables and optional local .env file.
 * Ensures security: API keys are never hard-coded or logged in plaintext.
 */
public class AppConfig {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;
    private final String agentMode; // "auto", "live", "mock"

    public AppConfig() {
        Map<String, String> dotEnv = loadDotEnv();

        this.apiKey = resolveConfig("OPENAI_API_KEY", dotEnv, "");
        this.baseUrl = resolveConfig("OPENAI_BASE_URL", dotEnv, DEFAULT_BASE_URL);
        this.model = resolveConfig("OPENAI_MODEL", dotEnv, DEFAULT_MODEL);
        this.agentMode = resolveConfig("AGENT_MODE", dotEnv, "auto");

        String timeoutStr = resolveConfig("HTTP_TIMEOUT_SECONDS", dotEnv, String.valueOf(DEFAULT_TIMEOUT_SECONDS));
        int parsedTimeout;
        try {
            parsedTimeout = Integer.parseInt(timeoutStr.trim());
        } catch (NumberFormatException e) {
            parsedTimeout = DEFAULT_TIMEOUT_SECONDS;
        }
        this.timeoutSeconds = parsedTimeout;
    }

    public AppConfig(String apiKey, String baseUrl, String model, int timeoutSeconds, String agentMode) {
        this.apiKey = apiKey != null ? apiKey : "";
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
        this.agentMode = agentMode != null ? agentMode : "auto";
    }

    private static String resolveConfig(String key, Map<String, String> dotEnv, String defaultValue) {
        // 1. System environment variable takes highest precedence
        String envVal = System.getenv(key);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        // 2. .env file value
        String fileVal = dotEnv.get(key);
        if (fileVal != null && !fileVal.trim().isEmpty()) {
            return fileVal.trim();
        }
        // 3. Fallback default
        return defaultValue;
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> envMap = new HashMap<>();
        File envFile = new File(".env");
        if (!envFile.exists() || !envFile.isFile()) {
            return envMap;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    // Strip quotes if present
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                        if (val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                    }
                    envMap.put(key, val);
                }
            }
        } catch (IOException ignored) {
            // Silently ignore .env read errors; defaults and env vars will be used
        }
        return envMap;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getAgentMode() {
        return agentMode;
    }

    public boolean hasValidApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("your_openai_or_compatible_api_key_here");
    }

    public boolean isLiveMode() {
        if ("mock".equalsIgnoreCase(agentMode)) {
            return false;
        }
        if ("live".equalsIgnoreCase(agentMode)) {
            return true;
        }
        // "auto" mode: live if key present, mock otherwise
        return hasValidApiKey();
    }

    /**
     * Safely returns a masked version of the API key for display/logging.
     */
    public String getMaskedApiKey() {
        if (!hasValidApiKey()) {
            return "[Not Configured / Offline Simulation Mode]";
        }
        if (apiKey.length() <= 8) {
            return "********";
        }
        return apiKey.substring(0, 3) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
