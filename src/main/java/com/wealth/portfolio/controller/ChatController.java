package com.wealth.portfolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wealth.portfolio.dto.ChatRequest;
import com.wealth.portfolio.dto.ChatResponse;
import com.wealth.portfolio.service.ClaudeService;
import com.wealth.portfolio.service.PortfolioContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final PortfolioContextService portfolioContextService;
    private final ClaudeService claudeService;

    public ChatController(PortfolioContextService portfolioContextService, ClaudeService claudeService) {
        this.portfolioContextService = portfolioContextService;
        this.claudeService = claudeService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            String tab = request.getTab() != null ? request.getTab() : "Portfolio Overview";
            String systemPrompt = buildSystemPrompt(tab, request.getTabContext());
            String reply = claudeService.chat(systemPrompt, request.getMessage());
            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Sorry, I encountered an error processing your request. Please try again."));
        }
    }

    // ── System prompt builders ──────────────────────────────────────────────────

    private String buildSystemPrompt(String tab, JsonNode tabContext) throws Exception {
        return switch (tab) {
            case "Watchlist"      -> buildWatchlistPrompt(tabContext);
            case "Subscriptions"  -> buildSubscriptionsPrompt(tabContext);
            case "Fundamentals"   -> buildFundamentalsPrompt(tabContext);
            default               -> buildPortfolioPrompt();
        };
    }

    private String buildPortfolioPrompt() throws Exception {
        String portfolioContext = portfolioContextService.buildPortfolioContext();
        return """
                You are a personal wealth management assistant. You have access to the user's
                current investment portfolio listed below. Answer questions accurately using only
                the data provided. Be concise and helpful. If a question cannot be answered from
                the portfolio data alone, say so clearly rather than guessing.

                """ + portfolioContext;
    }

    private String buildWatchlistPrompt(JsonNode tabContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a personal wealth management assistant focused on the user's stock watchlist.\n");
        sb.append("Today's date is ").append(LocalDate.now()).append(".\n\n");

        if (tabContext != null && tabContext.has("watchlist")) {
            JsonNode watchlist = tabContext.get("watchlist");
            if (watchlist.isArray() && !watchlist.isEmpty()) {
                sb.append("## Watched Stocks\n");
                for (JsonNode item : watchlist) {
                    String symbol  = item.path("symbol").asText("");
                    String name    = item.path("name").asText("");
                    String addedAt = item.path("addedAt").asText("");
                    sb.append(String.format("- %s (%s)%s%n",
                            symbol, name,
                            addedAt.isBlank() ? "" : " — added " + addedAt));
                }
            } else {
                sb.append("The user's watchlist is currently empty.\n");
            }
        } else {
            sb.append("No watchlist data available.\n");
        }

        sb.append("""

                Note: Real-time price data is visible in the Watchlist tab UI but is not included here.
                Answer questions about which stocks are being watched, why they may be interesting,
                general market context, or whether to add/remove stocks. Be concise and helpful.
                """);
        return sb.toString();
    }

    private String buildSubscriptionsPrompt(JsonNode tabContext) {
        LocalDate today      = LocalDate.now();
        int dayOfMonth       = today.getDayOfMonth();
        LocalDate weekEnd    = today.plusDays(6);
        boolean sameMonth    = weekEnd.getMonthValue() == today.getMonthValue();
        int weekEndDay       = weekEnd.getDayOfMonth();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a personal finance assistant focused on the user's subscriptions and scheduled payments.\n");
        sb.append(String.format("Today is %s (day %d of the month, %s).%n",
                today, dayOfMonth, today.getDayOfWeek().toString().charAt(0)
                + today.getDayOfWeek().toString().substring(1).toLowerCase()));
        if (sameMonth) {
            sb.append(String.format("This week covers billing days %d – %d of this month.%n%n", dayOfMonth, weekEndDay));
        } else {
            sb.append(String.format("This week spans the end of this month (from day %d) and the start of next month (until day %d).%n%n",
                    dayOfMonth, weekEndDay));
        }

        if (tabContext != null && tabContext.has("subscriptions")) {
            JsonNode subs = tabContext.get("subscriptions");
            if (subs.isArray() && !subs.isEmpty()) {
                sb.append("## Subscriptions & Scheduled Payments\n");
                for (JsonNode s : subs) {
                    String name        = s.path("name").asText("");
                    double amount      = s.path("amount").asDouble(0);
                    String category    = s.path("category").asText("");
                    String frequency   = s.path("frequency").asText("");
                    String billingDate = s.path("billingDate").asText("");
                    String status      = s.path("status").asText("");
                    String billing     = billingDate.isBlank() ? "No billing day set" : "Billing Day: " + billingDate;
                    sb.append(String.format("- %s | %s | SGD %.2f | %s | %s | %s%n",
                            name, category, amount, frequency, billing, status));
                }
            } else {
                sb.append("No subscriptions recorded yet.\n");
            }
        } else {
            sb.append("No subscription data available.\n");
        }

        sb.append(String.format("""

                A payment is "due this week" if its billing day falls between day %d and day %d.
                A payment may be "missed" if it is Active, has a billing day set, and that day is earlier
                than today (day %d) in the current month.
                Answer questions about upcoming payments, missed payments, total costs, and categories.
                Be concise and direct.
                """, dayOfMonth, sameMonth ? weekEndDay : 31, dayOfMonth));

        return sb.toString();
    }

    private String buildFundamentalsPrompt(JsonNode tabContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a financial analysis assistant focused on stock fundamental analysis.\n\n");

        if (tabContext != null && tabContext.has("stocks")) {
            JsonNode stocks = tabContext.get("stocks");
            if (stocks.isArray() && !stocks.isEmpty()) {
                for (JsonNode stock : stocks) {
                    String symbol = stock.path("symbol").asText("Unknown");
                    sb.append("## ").append(symbol).append("\n");
                    appendMetric(sb, "PE Ratio",              stock, "peRatio",               false);
                    appendMetric(sb, "P/B Ratio",             stock, "pbRatio",               false);
                    appendMetric(sb, "Dividend Yield",        stock, "dividendYield",         true);
                    appendMetric(sb, "Net Profit Margin",     stock, "netProfitMargin",       true);
                    appendMetric(sb, "Earnings/Book Value",   stock, "earningsToBookValue",   false);
                    appendMetric(sb, "PEG Ratio",             stock, "pegRatio",              false);
                    appendMetric(sb, "Current Ratio",         stock, "currentRatio",          false);
                    appendMetric(sb, "Working Capital/Debt",  stock, "workingCapitalToDebt",  false);
                    appendMetric(sb, "Net Current Asset/Debt",stock, "netCurrentAssetToDebt", false);
                    appendMetric(sb, "ROE",                   stock, "roe",                   true);
                    appendMetric(sb, "ROCE",                  stock, "roce",                  true);
                    sb.append("\n");
                }
            } else {
                sb.append("No stock fundamentals data is loaded. Ask the user to search for a stock in the Fundamentals tab first.\n\n");
            }
        } else {
            sb.append("No stock fundamentals data is loaded. The user needs to search for a stock in the Fundamentals tab first.\n\n");
        }

        sb.append("""
                Benchmark reference:
                - PE Ratio: < 15 Good, < 25 Moderate, ≥ 25 Poor
                - P/B Ratio: < 1.5 Good, < 3 Moderate, ≥ 3 Poor
                - Dividend Yield: ≥ 3% Good, ≥ 1% Moderate, < 1% Poor
                - Net Profit Margin: ≥ 20% Good, ≥ 10% Moderate, < 10% Poor
                - Earnings/Book Value: ≥ 1.5 Good, ≥ 0.5 Moderate, < 0.5 Poor
                - PEG Ratio: < 1 Good, < 2 Moderate, ≥ 2 Poor
                - Current Ratio: ≥ 2 Good, ≥ 1 Moderate, < 1 Poor
                - ROE: ≥ 15% Good, ≥ 8% Moderate, < 8% Poor
                - ROCE: ≥ 15% Good, ≥ 8% Moderate, < 8% Poor

                Answer questions about these metrics, compare stocks if two are loaded, and provide
                investment insights based on the data. N/A means data was unavailable from the source.
                Be concise and helpful.
                """);

        return sb.toString();
    }

    private void appendMetric(StringBuilder sb, String label, JsonNode stock, String key, boolean isPercent) {
        JsonNode val = stock.get(key);
        if (val == null || val.isNull()) {
            sb.append(String.format("- %s: N/A%n", label));
        } else {
            double d = val.asDouble();
            String formatted = isPercent ? String.format("%.2f%%", d) : String.format("%.2f", d);
            sb.append(String.format("- %s: %s%n", label, formatted));
        }
    }
}
