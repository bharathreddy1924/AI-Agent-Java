package com.aiagent.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppConfig Security and Configuration Tests")
class AppConfigTest {

    @Test
    @DisplayName("Should use safe defaults when no environment variables are set")
    void testDefaults() {
        AppConfig config = new AppConfig("", null, null, -1, "mock");

        assertEquals("https://api.openai.com/v1", config.getBaseUrl());
        assertEquals("gpt-4o-mini", config.getModel());
        assertEquals(30, config.getTimeoutSeconds());
        assertEquals("mock", config.getAgentMode());
        assertFalse(config.hasValidApiKey());
        assertFalse(config.isLiveMode());
    }

    @Test
    @DisplayName("Should mask API keys securely in logs/display")
    void testMaskedApiKey() {
        AppConfig emptyKeyConfig = new AppConfig("", null, null, 30, "mock");
        assertTrue(emptyKeyConfig.getMaskedApiKey().contains("Not Configured"));

        AppConfig validKeyConfig = new AppConfig("sk-proj-1234567890abcdef", null, null, 30, "live");
        String masked = validKeyConfig.getMaskedApiKey();
        assertTrue(masked.startsWith("sk-"));
        assertTrue(masked.endsWith("cdef"));
        assertFalse(masked.contains("1234567890"));
    }

    @Test
    @DisplayName("Should correctly determine live mode based on mode and key presence")
    void testIsLiveMode() {
        AppConfig liveConfig = new AppConfig("sk-test12345678", null, null, 30, "auto");
        assertTrue(liveConfig.isLiveMode());

        AppConfig forcedMockConfig = new AppConfig("sk-test12345678", null, null, 30, "mock");
        assertFalse(forcedMockConfig.isLiveMode());

        AppConfig noKeyAutoConfig = new AppConfig("", null, null, 30, "auto");
        assertFalse(noKeyAutoConfig.isLiveMode());
    }
}
