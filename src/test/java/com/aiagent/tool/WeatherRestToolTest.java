package com.aiagent.tool;

import com.aiagent.model.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WeatherRestTool Tests")
class WeatherRestToolTest {

    private WeatherRestTool weatherTool;

    @BeforeEach
    void setUp() {
        weatherTool = new WeatherRestTool();
    }

    @Test
    @DisplayName("Should successfully parse Open-Meteo JSON response")
    void testParseWeatherData() {
        String mockJson = """
                {
                    "latitude": 35.6895,
                    "longitude": 139.6917,
                    "current_weather": {
                        "temperature": 18.5,
                        "windspeed": 12.4,
                        "winddirection": 180,
                        "weathercode": 0,
                        "is_day": 1,
                        "time": "2026-09-29T12:00"
                    }
                }
                """;

        ToolResult result = weatherTool.parseWeatherData("Tokyo", "Japan", mockJson);

        assertNotNull(result);
        assertFalse(result.isError(), "Parsing valid weather JSON should succeed");
        assertTrue(result.getContent().contains("Tokyo"));
        assertTrue(result.getContent().contains("Japan"));
        assertTrue(result.getContent().contains("18.5"));
        assertTrue(result.getContent().contains("Clear sky"));
        assertTrue(result.getContent().contains("12.4"));
    }

    @Test
    @DisplayName("Should handle missing current_weather node gracefully")
    void testParseWeatherDataMissingNode() {
        String invalidJson = "{\"latitude\": 10.0, \"longitude\": 20.0}";
        ToolResult result = weatherTool.parseWeatherData("Unknown", "", invalidJson);

        assertNotNull(result);
        assertTrue(result.isError());
        assertTrue(result.getContent().contains("Weather data unavailable"));
    }

    @Test
    @DisplayName("Should handle malformed JSON during parsing")
    void testParseMalformedJson() {
        String badJson = "{invalid-json-body";
        ToolResult result = weatherTool.parseWeatherData("Nowhere", "", badJson);

        assertNotNull(result);
        assertTrue(result.isError());
        assertTrue(result.getContent().contains("Error parsing weather JSON"));
    }

    @ParameterizedTest(name = "Code {0} should map to \"{1}\"")
    @CsvSource({
            "0, Clear sky",
            "1, Mainly clear",
            "2, Partly cloudy",
            "3, Overcast",
            "45, Foggy",
            "61, Rain",
            "71, Snowfall",
            "95, Thunderstorm",
            "999, Unknown (999)"
    })
    @DisplayName("Should accurately map WMO weather codes to human descriptions")
    void testWeatherCodeMapping(int code, String expectedDescription) {
        String description = WeatherRestTool.mapWeatherCodeToDescription(code);
        assertEquals(expectedDescription, description);
    }

    @Test
    @DisplayName("Should reject blank or null city arguments")
    void testInvalidCityArguments() {
        ToolResult nullRes = weatherTool.execute(null);
        assertTrue(nullRes.isError());

        ToolResult emptyRes = weatherTool.execute("{\"city\":\"\"}");
        assertTrue(emptyRes.isError());

        ToolResult missingProp = weatherTool.execute("{\"country\":\"France\"}");
        assertTrue(missingProp.isError());
        assertTrue(missingProp.getContent().contains("Missing required argument 'city'"));
    }

    @Test
    @DisplayName("Should provide valid OpenAI parameter schema")
    void testParameterSchema() {
        Map<String, Object> schema = weatherTool.getParameterSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
        assertTrue(schema.containsKey("properties"));
        assertTrue(schema.containsKey("required"));
    }
}
