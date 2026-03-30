package com.wealth.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealth.portfolio.mcp.PortfolioMcpTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeService {

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL             = "claude-sonnet-4-6";
    private static final int    MAX_TOKENS        = 2048;
    private static final int    MAX_TOOL_ROUNDS   = 5;  // prevent infinite tool loops

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaudeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ── Simple chat (no tools) — used for Watchlist & Subscriptions tabs ─────────

    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("model",      MODEL);
        body.put("max_tokens", MAX_TOKENS);
        body.put("system",     systemPrompt);
        body.put("messages",   List.of(Map.of("role", "user", "content", userMessage)));

        try {
            JsonNode response = callClaude(body);
            return extractText(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Claude API: " + e.getMessage(), e);
        }
    }

    // ── Agentic chat with tools — Claude fetches live data autonomously ───────────

    /**
     * Sends the user message to Claude along with tool definitions.
     * When Claude returns tool_use blocks the tools are executed and results are fed back.
     * The loop continues until Claude produces a final text response (stop_reason = end_turn).
     */
    public String chatWithTools(String systemPrompt, String userMessage, PortfolioMcpTools mcpTools) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", userMessage));

        List<Map<String, Object>> tools = mcpTools.getClaudeToolDefinitions();

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            Map<String, Object> body = new HashMap<>();
            body.put("model",      MODEL);
            body.put("max_tokens", MAX_TOKENS);
            body.put("system",     systemPrompt);
            body.put("tools",      tools);
            body.put("messages",   messages);

            try {
                JsonNode response   = callClaude(body);
                String   stopReason = response.path("stop_reason").asText("end_turn");
                JsonNode content    = response.get("content");

                if (!"tool_use".equals(stopReason)) {
                    return extractText(response);
                }

                // Claude requested one or more tool calls — execute them all
                List<Map<String, Object>> assistantContent = new ArrayList<>();
                List<Map<String, Object>> toolResults      = new ArrayList<>();

                for (JsonNode block : content) {
                    String type = block.path("type").asText();

                    if ("text".equals(type)) {
                        Map<String, Object> textBlock = new HashMap<>();
                        textBlock.put("type", "text");
                        textBlock.put("text", block.path("text").asText(""));
                        assistantContent.add(textBlock);

                    } else if ("tool_use".equals(type)) {
                        String   toolId   = block.path("id").asText();
                        String   toolName = block.path("name").asText();
                        JsonNode input    = block.has("input") ? block.get("input")
                                                               : objectMapper.createObjectNode();

                        // Reconstruct tool_use block for the assistant message
                        Map<String, Object> toolUseBlock = new HashMap<>();
                        toolUseBlock.put("type",  "tool_use");
                        toolUseBlock.put("id",    toolId);
                        toolUseBlock.put("name",  toolName);
                        toolUseBlock.put("input", objectMapper.convertValue(input, Object.class));
                        assistantContent.add(toolUseBlock);

                        // Execute and collect result
                        String result = mcpTools.executeTool(toolName, input);

                        Map<String, Object> toolResult = new HashMap<>();
                        toolResult.put("type",        "tool_result");
                        toolResult.put("tool_use_id", toolId);
                        toolResult.put("content",     result);
                        toolResults.add(toolResult);
                    }
                }

                // Append assistant turn (contains tool_use blocks) to message history
                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role",    "assistant");
                assistantMsg.put("content", assistantContent);
                messages.add(assistantMsg);

                // Append tool results as a user turn
                Map<String, Object> toolResultMsg = new HashMap<>();
                toolResultMsg.put("role",    "user");
                toolResultMsg.put("content", toolResults);
                messages.add(toolResultMsg);

            } catch (Exception e) {
                throw new RuntimeException("Claude API error in tool loop (round " + round + "): " + e.getMessage(), e);
            }
        }

        return "I reached the maximum number of tool calls. Please try rephrasing your question.";
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private JsonNode callClaude(Map<String, Object> body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key",         apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String rawResponse = restTemplate.postForObject(apiUrl, request, String.class);
        return objectMapper.readTree(rawResponse);
    }

    private String extractText(JsonNode response) {
        JsonNode content = response.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText();
                }
            }
        }
        return "I could not generate a response. Please try again.";
    }
}
