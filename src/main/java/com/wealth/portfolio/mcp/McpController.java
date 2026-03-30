package com.wealth.portfolio.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP (Model Context Protocol) server using SSE transport.
 *
 * External MCP clients (e.g. Claude Desktop) connect to:
 *   GET  /mcp/sse          — open SSE stream, receive session endpoint
 *   POST /mcp/messages     — send JSON-RPC 2.0 requests; responses arrive via SSE
 *
 * Supported JSON-RPC methods:
 *   initialize              — handshake, returns server capabilities
 *   notifications/initialized — client acknowledgement (no response)
 *   tools/list              — list available tools
 *   tools/call              — execute a tool and return result
 *
 * To connect from Claude Desktop, add to claude_desktop_config.json:
 * {
 *   "mcpServers": {
 *     "wealth-portfolio": {
 *       "url": "http://localhost:8010/mcp/sse",
 *       "transport": "sse"
 *     }
 *   }
 * }
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final String MCP_PROTOCOL_VERSION = "2024-11-05";
    private static final String SERVER_NAME    = "wealth-portfolio-mcp";
    private static final String SERVER_VERSION = "1.0.0";

    private final PortfolioMcpTools mcpTools;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Active SSE sessions keyed by sessionId
    private final ConcurrentHashMap<String, SseEmitter> sessions = new ConcurrentHashMap<>();

    public McpController(PortfolioMcpTools mcpTools) {
        this.mcpTools = mcpTools;
    }

    // ── SSE handshake endpoint ───────────────────────────────────────────────────

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter openSseSession() {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L); // no timeout

        sessions.put(sessionId, emitter);
        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> sessions.remove(sessionId));
        emitter.onError(e -> sessions.remove(sessionId));

        try {
            // Tell the client which URL to POST messages to
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data("/mcp/messages?sessionId=" + sessionId));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // ── Message endpoint (JSON-RPC over SSE) ─────────────────────────────────────

    @PostMapping("/messages")
    public ResponseEntity<Void> handleMessage(
            @RequestParam String sessionId,
            @RequestBody String body) {

        SseEmitter emitter = sessions.get(sessionId);
        if (emitter == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            JsonNode req   = objectMapper.readTree(body);
            String  method = req.path("method").asText("");
            JsonNode idNode = req.get("id");
            Object  id     = parseId(idNode);

            // notifications have no id and require no response
            if (method.startsWith("notifications/")) {
                return ResponseEntity.ok().build();
            }

            Object response = switch (method) {
                case "initialize"  -> buildInitializeResponse(id, req.path("params"));
                case "tools/list"  -> buildToolsListResponse(id);
                case "tools/call"  -> buildToolsCallResponse(id, req.path("params"));
                default            -> buildErrorResponse(id, -32601, "Method not found: " + method);
            };

            sendSse(emitter, response);

        } catch (Exception e) {
            try {
                sendSse(emitter, buildErrorResponse(null, -32700, "Parse error: " + e.getMessage()));
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok().build();
    }

    // ── JSON-RPC handlers ────────────────────────────────────────────────────────

    private Map<String, Object> buildInitializeResponse(Object id, JsonNode params) {
        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("name",    SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", Map.of());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", MCP_PROTOCOL_VERSION);
        result.put("capabilities",    capabilities);
        result.put("serverInfo",      serverInfo);

        return rpcResult(id, result);
    }

    private Map<String, Object> buildToolsListResponse(Object id) {
        List<Map<String, Object>> tools = mcpTools.getMcpToolDefinitions();
        return rpcResult(id, Map.of("tools", tools));
    }

    private Map<String, Object> buildToolsCallResponse(Object id, JsonNode params) {
        String   toolName = params.path("name").asText("");
        JsonNode input    = params.has("arguments") ? params.get("arguments")
                                                    : objectMapper.createObjectNode();
        try {
            String text = mcpTools.executeTool(toolName, input);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content",  List.of(Map.of("type", "text", "text", text)));
            result.put("isError",  false);
            return rpcResult(id, result);
        } catch (Exception e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content",  List.of(Map.of("type", "text", "text", "Tool error: " + e.getMessage())));
            result.put("isError",  true);
            return rpcResult(id, result);
        }
    }

    // ── JSON-RPC helpers ─────────────────────────────────────────────────────────

    private Map<String, Object> rpcResult(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id",      id);
        response.put("result",  result);
        return response;
    }

    private Map<String, Object> buildErrorResponse(Object id, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code",    code);
        error.put("message", message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id",      id);
        response.put("error",   error);
        return response;
    }

    private void sendSse(SseEmitter emitter, Object payload) throws Exception {
        emitter.send(SseEmitter.event()
                .name("message")
                .data(objectMapper.writeValueAsString(payload)));
    }

    private Object parseId(JsonNode idNode) {
        if (idNode == null || idNode.isNull()) return null;
        if (idNode.isInt())    return idNode.intValue();
        if (idNode.isLong())   return idNode.longValue();
        if (idNode.isTextual()) return idNode.textValue();
        return idNode.asText();
    }
}
