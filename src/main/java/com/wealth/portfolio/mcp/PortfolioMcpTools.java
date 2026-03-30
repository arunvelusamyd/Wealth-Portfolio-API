package com.wealth.portfolio.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.wealth.portfolio.dto.StockFundamentalsResponse;
import com.wealth.portfolio.dto.StockQuoteResponse;
import com.wealth.portfolio.dto.TechnicalAnalysisResponse;
import com.wealth.portfolio.dto.TickerSearchResult;
import com.wealth.portfolio.service.FinnhubService;
import com.wealth.portfolio.service.PortfolioContextService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wealth portfolio tools exposed via:
 *  1. MCP server (McpController) — for Claude Desktop / external MCP clients
 *  2. Claude tool-calling (ClaudeService) — for the in-app chat agentic loop
 */
@Service
public class PortfolioMcpTools {

    private final FinnhubService finnhubService;
    private final PortfolioContextService portfolioContextService;

    public PortfolioMcpTools(FinnhubService finnhubService, PortfolioContextService portfolioContextService) {
        this.finnhubService = finnhubService;
        this.portfolioContextService = portfolioContextService;
    }

    // ── Tool implementations ─────────────────────────────────────────────────────

    public String getPortfolioSummary() {
        try {
            return portfolioContextService.buildPortfolioContext();
        } catch (Exception e) {
            return "Error fetching portfolio: " + e.getMessage();
        }
    }

    public String getTechnicalAnalysis(String symbol, String resolution) {
        if (symbol == null || symbol.isBlank()) return "Symbol is required.";
        String res = (resolution == null || resolution.isBlank()) ? "D" : resolution;
        try {
            TechnicalAnalysisResponse r = finnhubService.getTechnicalAnalysis(symbol.trim().toUpperCase(), res);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Technical Analysis: %s (%s resolution)\n", r.getSymbol(), r.getResolution()));
            sb.append(String.format("Current Price: $%.2f | Change: %+.2f (%+.2f%%)\n",
                    r.getCurrentPrice(), r.getChange(), r.getPercentChange()));
            sb.append(String.format("Open: $%.2f | High: $%.2f | Low: $%.2f | Prev Close: $%.2f\n\n",
                    r.getOpen(), r.getHigh(), r.getLow(), r.getPreviousClose()));

            List<Double> resistance = r.getResistanceLevels();
            if (!resistance.isEmpty()) {
                sb.append("Resistance Levels (nearest first):\n");
                for (int i = 0; i < resistance.size(); i++) {
                    double lvl = resistance.get(i);
                    double pct = ((lvl - r.getCurrentPrice()) / r.getCurrentPrice()) * 100;
                    sb.append(String.format("  R%d: $%.2f (+%.2f%%)\n", i + 1, lvl, pct));
                }
            } else {
                sb.append("No resistance levels identified.\n");
            }

            List<Double> support = r.getSupportLevels();
            if (!support.isEmpty()) {
                sb.append("Support Levels (nearest first):\n");
                for (int i = 0; i < support.size(); i++) {
                    double lvl = support.get(i);
                    double pct = ((lvl - r.getCurrentPrice()) / r.getCurrentPrice()) * 100;
                    sb.append(String.format("  S%d: $%.2f (%.2f%%)\n", i + 1, lvl, pct));
                }
            } else {
                sb.append("No support levels identified.\n");
            }

            if (r.getRsi() != null) {
                sb.append(String.format("\nRSI (14): %.1f — %s\n", r.getRsi(), r.getRsiSignal()));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Could not fetch technical analysis for " + symbol + ": " + e.getMessage();
        }
    }

    public String getStockFundamentals(String symbol) {
        if (symbol == null || symbol.isBlank()) return "Symbol is required.";
        try {
            StockFundamentalsResponse r = finnhubService.getFundamentals(symbol.trim().toUpperCase());
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Fundamentals: %s\n", r.getSymbol()));
            appendFmt(sb, "PE Ratio",           r.getPeRatio(),           false);
            appendFmt(sb, "P/B Ratio",           r.getPbRatio(),           false);
            appendFmt(sb, "Dividend Yield",      r.getDividendYield(),     true);
            appendFmt(sb, "Net Profit Margin",   r.getNetProfitMargin(),   true);
            appendFmt(sb, "Earnings/Book Value", r.getEarningsToBookValue(), false);
            appendFmt(sb, "PEG Ratio",           r.getPegRatio(),          false);
            appendFmt(sb, "Current Ratio",       r.getCurrentRatio(),      false);
            appendFmt(sb, "ROE",                 r.getRoe(),               true);
            appendFmt(sb, "ROCE",                r.getRoce(),              true);
            return sb.toString();
        } catch (Exception e) {
            return "Could not fetch fundamentals for " + symbol + ": " + e.getMessage();
        }
    }

    public String getStockQuote(String symbol) {
        if (symbol == null || symbol.isBlank()) return "Symbol is required.";
        try {
            StockQuoteResponse r = finnhubService.getStockQuote(symbol.trim().toUpperCase());
            return String.format("%s: $%.2f | Change: %+.2f (%+.2f%%) | High: $%.2f | Low: $%.2f | Open: $%.2f | Prev Close: $%.2f",
                    symbol.toUpperCase(), r.getCurrentPrice(), r.getChange(), r.getPercentChange(),
                    r.getHigh(), r.getLow(), r.getOpen(), r.getPreviousClose());
        } catch (Exception e) {
            return "Could not fetch quote for " + symbol + ": " + e.getMessage();
        }
    }

    public String searchStock(String query) {
        if (query == null || query.isBlank()) return "Query is required.";
        try {
            List<TickerSearchResult> results = finnhubService.searchSymbols(query.trim());
            if (results.isEmpty()) return "No results found for: " + query;
            StringBuilder sb = new StringBuilder("Search results for \"" + query + "\":\n");
            results.forEach(r -> sb.append(String.format("  %s - %s (%s)\n",
                    r.getSymbol(), r.getDescription(), r.getType())));
            return sb.toString();
        } catch (Exception e) {
            return "Search failed for " + query + ": " + e.getMessage();
        }
    }

    // ── Tool dispatch (used by both MCP server and ClaudeService) ────────────────

    public String executeTool(String name, JsonNode input) {
        return switch (name) {
            case "get_portfolio_summary"  -> getPortfolioSummary();
            case "get_technical_analysis" -> getTechnicalAnalysis(
                    input.path("symbol").asText(""),
                    input.path("resolution").asText("D"));
            case "get_stock_fundamentals" -> getStockFundamentals(input.path("symbol").asText(""));
            case "get_stock_quote"        -> getStockQuote(input.path("symbol").asText(""));
            case "search_stock"           -> searchStock(input.path("query").asText(""));
            default                       -> "Unknown tool: " + name;
        };
    }

    // ── Tool definitions for Claude API (input_schema) ───────────────────────────

    public List<Map<String, Object>> getClaudeToolDefinitions() {
        return List.of(
            claudeTool("get_portfolio_summary",
                "Retrieve the user's full investment portfolio: stocks, ETFs, unit trusts, cash balances with unrealised P&L",
                Map.of("type", "object", "properties", Map.of(), "required", List.of())),

            claudeTool("get_technical_analysis",
                "Get support/resistance levels and RSI indicator for any stock symbol",
                Map.of("type", "object",
                    "properties", Map.of(
                        "symbol", Map.of("type", "string", "description", "Ticker symbol, e.g. AAPL, TSLA, NVDA"),
                        "resolution", Map.of("type", "string", "description", "D = daily, W = weekly, M = monthly",
                                "enum", List.of("D", "W", "M"))),
                    "required", List.of("symbol"))),

            claudeTool("get_stock_fundamentals",
                "Get fundamental metrics for a stock: PE ratio, P/B, dividend yield, profit margin, ROE, ROCE, PEG ratio",
                Map.of("type", "object",
                    "properties", Map.of(
                        "symbol", Map.of("type", "string", "description", "Ticker symbol, e.g. AAPL")),
                    "required", List.of("symbol"))),

            claudeTool("get_stock_quote",
                "Get the real-time price quote for a stock symbol",
                Map.of("type", "object",
                    "properties", Map.of(
                        "symbol", Map.of("type", "string", "description", "Ticker symbol, e.g. AAPL")),
                    "required", List.of("symbol"))),

            claudeTool("search_stock",
                "Search for a stock symbol by company name or partial ticker string",
                Map.of("type", "object",
                    "properties", Map.of(
                        "query", Map.of("type", "string", "description", "Company name or ticker to search for")),
                    "required", List.of("query")))
        );
    }

    // ── Tool definitions for MCP protocol (inputSchema) ─────────────────────────

    public List<Map<String, Object>> getMcpToolDefinitions() {
        return List.of(
            mcpTool("get_portfolio_summary",
                "Retrieve the user's full investment portfolio: stocks, ETFs, unit trusts, cash balances",
                Map.of("type", "object", "properties", Map.of())),

            mcpTool("get_technical_analysis",
                "Get support/resistance levels and RSI for a stock",
                Map.of("type", "object",
                    "properties", Map.of(
                        "symbol",     Map.of("type", "string", "description", "Ticker symbol e.g. AAPL"),
                        "resolution", Map.of("type", "string", "description", "D/W/M", "enum", List.of("D","W","M"))),
                    "required", List.of("symbol"))),

            mcpTool("get_stock_fundamentals",
                "Get PE, PB, dividend yield, profit margin, ROE, ROCE for a stock",
                Map.of("type", "object",
                    "properties", Map.of(
                        "symbol", Map.of("type", "string", "description", "Ticker symbol")),
                    "required", List.of("symbol"))),

            mcpTool("get_stock_quote",
                "Get real-time price quote for a stock",
                Map.of("type", "object",
                    "properties", Map.of(
                        "symbol", Map.of("type", "string", "description", "Ticker symbol")),
                    "required", List.of("symbol"))),

            mcpTool("search_stock",
                "Search for a stock by company name or ticker",
                Map.of("type", "object",
                    "properties", Map.of(
                        "query", Map.of("type", "string", "description", "Search term")),
                    "required", List.of("query")))
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Map<String, Object> claudeTool(String name, String description, Map<String, Object> inputSchema) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("name", name);
        t.put("description", description);
        t.put("input_schema", inputSchema);
        return t;
    }

    private Map<String, Object> mcpTool(String name, String description, Map<String, Object> inputSchema) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("name", name);
        t.put("description", description);
        t.put("inputSchema", inputSchema);
        return t;
    }

    private void appendFmt(StringBuilder sb, String label, Double value, boolean isPercent) {
        if (value == null) {
            sb.append(String.format("- %s: N/A\n", label));
        } else if (isPercent) {
            sb.append(String.format("- %s: %.2f%%\n", label, value));
        } else {
            sb.append(String.format("- %s: %.2f\n", label, value));
        }
    }
}
