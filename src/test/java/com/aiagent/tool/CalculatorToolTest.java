package com.aiagent.tool;

import com.aiagent.model.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CalculatorTool Tests")
class CalculatorToolTest {

    private CalculatorTool calculator;

    @BeforeEach
    void setUp() {
        calculator = new CalculatorTool();
    }

    @Test
    @DisplayName("Should multiply 125 by 48 and return 6000")
    void testMultiply125By48() {
        String inputJson = "{\"operation\":\"multiply\",\"a\":125,\"b\":48}";
        ToolResult result = calculator.execute(inputJson);

        assertNotNull(result);
        assertFalse(result.isError(), "Calculation should not fail");
        assertEquals("6000", result.getContent());
    }

    @ParameterizedTest(name = "{0}: {1} {2} {3} = {4}")
    @CsvSource({
            "Addition, add, 15, 27, 42",
            "Subtraction, subtract, 50, 18, 32",
            "Multiplication, multiply, 12, 11, 132",
            "Division, divide, 100, 4, 25",
            "Decimal Division, divide, 5, 2, 2.5",
            "Negative Numbers, add, -10, -5, -15"
    })
    @DisplayName("Should execute basic arithmetic operations accurately")
    void testArithmeticOperations(String label, String op, double a, double b, String expected) {
        String inputJson = String.format("{\"operation\":\"%s\",\"a\":%f,\"b\":%f}", op, a, b);
        ToolResult result = calculator.execute(inputJson);

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(expected, result.getContent());
    }

    @Test
    @DisplayName("Should handle division by zero with a descriptive error")
    void testDivisionByZero() {
        String inputJson = "{\"operation\":\"divide\",\"a\":42,\"b\":0}";
        ToolResult result = calculator.execute(inputJson);

        assertNotNull(result);
        assertTrue(result.isError(), "Division by zero must return an error result");
        assertTrue(result.getContent().contains("Division by zero is not allowed"),
                "Expected error message regarding division by zero, got: " + result.getContent());
    }

    @Test
    @DisplayName("Should reject invalid or unsupported operations")
    void testInvalidOperation() {
        String inputJson = "{\"operation\":\"exponentiate\",\"a\":2,\"b\":3}";
        ToolResult result = calculator.execute(inputJson);

        assertNotNull(result);
        assertTrue(result.isError());
        assertTrue(result.getContent().contains("Unsupported operation"));
    }

    @Test
    @DisplayName("Should handle missing operands")
    void testMissingOperands() {
        String inputJson = "{\"operation\":\"add\",\"a\":10}";
        ToolResult result = calculator.execute(inputJson);

        assertNotNull(result);
        assertTrue(result.isError());
        assertTrue(result.getContent().contains("Missing or invalid numeric operands"));
    }

    @Test
    @DisplayName("Should handle null and empty input")
    void testEmptyAndNullInput() {
        ToolResult resNull = calculator.execute(null);
        assertTrue(resNull.isError());

        ToolResult resEmpty = calculator.execute("   ");
        assertTrue(resEmpty.isError());
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void testMalformedJson() {
        ToolResult result = calculator.execute("{not-a-valid-json");
        assertNotNull(result);
        assertTrue(result.isError());
        assertTrue(result.getContent().contains("Error parsing"));
    }

    @Test
    @DisplayName("Should evaluate arithmetic expression fallback")
    void testExpressionFallback() {
        String inputJson = "{\"expression\":\"125 * 48\"}";
        ToolResult result = calculator.execute(inputJson);

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals("6000", result.getContent());
    }

    @Test
    @DisplayName("Should provide valid OpenAI parameter schema")
    void testParameterSchema() {
        Map<String, Object> schema = calculator.getParameterSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
        assertTrue(schema.containsKey("properties"));
        assertTrue(schema.containsKey("required"));
    }
}
