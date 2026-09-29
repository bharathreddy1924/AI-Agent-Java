package com.aiagent.tool;

import com.aiagent.model.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public REST API Tool that queries real-time weather information from Open-Meteo.
 * Does not require any API keys.
 * Demonstrates HTTP requests, JSON parsing, timeout handling, and error handling.
 */
public class WeatherRestTool implements Tool {

    private static final String TOOL_NAME = "get_weather";
    private static final String GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_BASE_URL = "https://api.open-meteo.com/v1/forecast";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public WeatherRestTool() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build(),
                new ObjectMapper(),
                Duration.ofSeconds(10));
    }

    public WeatherRestTool(HttpClient httpClient, ObjectMapper objectMapper, Duration timeout) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(10);
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "Retrieves current live weather information (temperature, weather conditions, windspeed) " +
                "for any given city worldwide using the free Open-Meteo public REST API.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        Map<String, Object> cityProp = new LinkedHashMap<>();
        cityProp.put("type", "string");
        cityProp.put("description", "The name of the city (e.g. 'Tokyo', 'London', 'New York', 'Paris')");
        properties.put("city", cityProp);

        schema.put("properties", properties);
        schema.put("required", List.of("city"));
        return schema;
    }

    @Override
    public ToolResult execute(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.trim().isEmpty()) {
            return ToolResult.error(TOOL_NAME, "Error: Arguments cannot be empty. Please provide a 'city'.");
        }

        String city;
        try {
            JsonNode root = objectMapper.readTree(argumentsJson);
            if (root.hasNonNull("city")) {
                city = root.get("city").asText().trim();
            } else if (root.hasNonNull("location")) {
                city = root.get("location").asText().trim();
            } else {
                return ToolResult.error(TOOL_NAME, "Error: Missing required argument 'city'.");
            }
        } catch (Exception e) {
            return ToolResult.error(TOOL_NAME, "Error parsing tool arguments JSON: " + e.getMessage());
        }

        if (city.isEmpty()) {
            return ToolResult.error(TOOL_NAME, "Error: 'city' argument cannot be blank.");
        }

        try {
            return fetchWeatherForCity(city);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.error(TOOL_NAME, "Error: Request interrupted while fetching weather for '" + city + "'.");
        } catch (Exception e) {
            return ToolResult.error(TOOL_NAME, "Error fetching weather data: " + e.getMessage());
        }
    }

    public ToolResult fetchWeatherForCity(String city) throws Exception {
        // Step 1: Geocoding lookup to find coordinates for the city
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String geoUri = GEOCODING_BASE_URL + "?name=" + encodedCity + "&count=1&language=en&format=json";

        HttpRequest geoRequest = HttpRequest.newBuilder()
                .uri(URI.create(geoUri))
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", "AI-Workflow-Agent/1.0")
                .GET()
                .build();

        HttpResponse<String> geoResponse;
        try {
            geoResponse = httpClient.send(geoRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return ToolResult.error(TOOL_NAME, "HTTP Request Failed (Geocoding API): " + e.getMessage());
        }

        if (geoResponse.statusCode() != 200) {
            return ToolResult.error(TOOL_NAME, "Geocoding API error (HTTP " + geoResponse.statusCode() + "): " + geoResponse.body());
        }

        JsonNode geoJson;
        try {
            geoJson = objectMapper.readTree(geoResponse.body());
        } catch (Exception e) {
            return ToolResult.error(TOOL_NAME, "Invalid JSON received from Geocoding API: " + e.getMessage());
        }

        if (!geoJson.has("results") || !geoJson.get("results").isArray() || geoJson.get("results").isEmpty()) {
            return ToolResult.error(TOOL_NAME, "Location not found: Could not find geographic coordinates for '" + city + "'.");
        }

        JsonNode locationNode = geoJson.get("results").get(0);
        double latitude = locationNode.path("latitude").asDouble();
        double longitude = locationNode.path("longitude").asDouble();
        String resolvedName = locationNode.path("name").asText(city);
        String country = locationNode.path("country").asText("");

        // Step 2: Fetch current weather for the coordinates
        String weatherUri = WEATHER_BASE_URL + "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&current_weather=true";

        HttpRequest weatherRequest = HttpRequest.newBuilder()
                .uri(URI.create(weatherUri))
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", "AI-Workflow-Agent/1.0")
                .GET()
                .build();

        HttpResponse<String> weatherResponse;
        try {
            weatherResponse = httpClient.send(weatherRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return ToolResult.error(TOOL_NAME, "HTTP Request Failed (Weather API): " + e.getMessage());
        }

        if (weatherResponse.statusCode() != 200) {
            return ToolResult.error(TOOL_NAME, "Weather API error (HTTP " + weatherResponse.statusCode() + "): " + weatherResponse.body());
        }

        return parseWeatherData(resolvedName, country, weatherResponse.body());
    }

    public ToolResult parseWeatherData(String city, String country, String jsonBody) {
        try {
            JsonNode weatherJson = objectMapper.readTree(jsonBody);
            JsonNode currentWeather = weatherJson.path("current_weather");

            if (currentWeather.isMissingNode() || currentWeather.isNull()) {
                return ToolResult.error(TOOL_NAME, "Weather data unavailable in API response for " + city);
            }

            double temperature = currentWeather.path("temperature").asDouble();
            double windspeed = currentWeather.path("windspeed").asDouble();
            int weatherCode = currentWeather.path("weathercode").asInt(-1);
            String time = currentWeather.path("time").asText("N/A");
            String condition = mapWeatherCodeToDescription(weatherCode);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("city", city);
            if (!country.isEmpty()) {
                summary.put("country", country);
            }
            summary.put("temperature_celsius", temperature);
            summary.put("condition", condition);
            summary.put("windspeed_kmh", windspeed);
            summary.put("observation_time", time);

            String formattedOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary);
            return ToolResult.success(TOOL_NAME, formattedOutput);

        } catch (Exception e) {
            return ToolResult.error(TOOL_NAME, "Error parsing weather JSON: " + e.getMessage());
        }
    }

    public static String mapWeatherCodeToDescription(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing Rain";
            case 71, 73, 75, 77 -> "Snowfall";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown (" + code + ")";
        };
    }
}
