package com.aiagent.tool;

import com.aiagent.model.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculator Tool supporting addition, subtraction, multiplication, and division.
 * Handles division by zero, invalid input, and various argument structures.
 */
public class CalculatorTool implements Tool {

    private static final String TOOL_NAME = "calculator";
    private final ObjectMapper objectMapper;

    public CalculatorTool() {
        this.objectMapper = new ObjectMapper();
    }

    public CalculatorTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "Performs arithmetic calculations including addition, subtraction, multiplication, and division. " +
                "Accepts an operation ('add', 'subtract', 'multiply', 'divide') and two numeric operands 'a' and 'b'.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> opProp = new LinkedHashMap<>();
        opProp.put("type", "string");
        opProp.put("enum", List.of("add", "subtract", "multiply", "divide"));
        opProp.put("description", "The arithmetic operation: 'add', 'subtract', 'multiply', or 'divide'");
        properties.put("operation", opProp);

        Map<String, Object> aProp = new LinkedHashMap<>();
        aProp.put("type", "number");
        aProp.put("description", "The first number (operand a)");
        properties.put("a", aProp);

        Map<String, Object> bProp = new LinkedHashMap<>();
        bProp.put("type", "number");
        bProp.put("description", "The second number (operand b)");
        properties.put("b", bProp);

        schema.put("properties", properties);
        schema.put("required", List.of("operation", "a", "b"));
        return schema;
    }

    @Override
    public ToolResult execute(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.trim().isEmpty()) {
            return ToolResult.error(TOOL_NAME, "Error: Calculator arguments cannot be empty.");
        }

        try {
            JsonNode root = objectMapper.readTree(argumentsJson);

            // Handle expression fallback if passed (e.g. {"expression": "125 * 48"})
            if (root.hasNonNull("expression")) {
                return evaluateExpression(root.get("expression").asText());
            }

            // Extract operation
            String operation = null;
            if (root.hasNonNull("operation")) {
                operation = root.get("operation").asText().trim().toLowerCase();
            } else if (root.hasNonNull("op")) {
                operation = root.get("op").asText().trim().toLowerCase();
            }

            if (operation == null || operation.isEmpty()) {
                return ToolResult.error(TOOL_NAME, "Error: Missing required argument 'operation'. Must be one of: add, subtract, multiply, divide.");
            }

            // Extract operands
            Double a = extractNumber(root, "a", "num1", "first");
            Double b = extractNumber(root, "b", "num2", "second");

            if (a == null || b == null) {
                return ToolResult.error(TOOL_NAME, "Error: Missing or invalid numeric operands 'a' and 'b'.");
            }

            return calculate(operation, a, b);

        } catch (Exception e) {
            return ToolResult.error(TOOL_NAME, "Error parsing calculator arguments: " + e.getMessage());
        }
    }

    public ToolResult calculate(String operation, double a, double b) {
        double result;
        String normalizedOp = normalizeOperation(operation);

        switch (normalizedOp) {
            case "add":
                result = a + b;
                break;
            case "subtract":
                result = a - b;
                break;
            case "multiply":
                result = a * b;
                break;
            case "divide":
                if (b == 0.0) {
                    return ToolResult.error(TOOL_NAME, "Error: Division by zero is not allowed.");
                }
                result = a / b;
                break;
            default:
                return ToolResult.error(TOOL_NAME, "Error: Unsupported operation '" + operation + "'. Supported: add, subtract, multiply, divide.");
        }

        if (Double.isNaN(result) || Double.isInfinite(result)) {
            return ToolResult.error(TOOL_NAME, "Error: Calculation resulted in an invalid number.");
        }

        String formattedResult = formatNumber(result);
        return ToolResult.success(TOOL_NAME, formattedResult);
    }

    private ToolResult evaluateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return ToolResult.error(TOOL_NAME, "Error: Expression cannot be empty.");
        }
        String expr = expression.replaceAll("\\s+", "");
        // Match simple binary expressions: <num> <op> <num>
        for (char op : new char[]{'+', '-', '*', '/', 'x'}) {
            int opIdx = expr.indexOf(op, 1); // skip leading minus
            if (opIdx > 0) {
                try {
                    double a = Double.parseDouble(expr.substring(0, opIdx));
                    double b = Double.parseDouble(expr.substring(opIdx + 1));
                    String opName = switch (op) {
                        case '+' -> "add";
                        case '-' -> "subtract";
                        case '*', 'x' -> "multiply";
                        case '/' -> "divide";
                        default -> "";
                    };
                    return calculate(opName, a, b);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ToolResult.error(TOOL_NAME, "Error: Unable to evaluate expression '" + expression + "'. Please provide operation, a, and b.");
    }

    private String normalizeOperation(String op) {
        if (op == null) return "";
        return switch (op.toLowerCase().trim()) {
            case "add", "addition", "plus", "+" -> "add";
            case "subtract", "subtraction", "minus", "-" -> "subtract";
            case "multiply", "multiplication", "times", "*", "x" -> "multiply";
            case "divide", "division", "/" -> "divide";
            default -> op.toLowerCase().trim();
        };
    }

    private Double extractNumber(JsonNode root, String... fieldNames) {
        for (String field : fieldNames) {
            if (root.hasNonNull(field)) {
                JsonNode node = root.get(field);
                if (node.isNumber()) {
                    return node.asDouble();
                } else if (node.isTextual()) {
                    try {
                        return Double.parseDouble(node.asText().trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
