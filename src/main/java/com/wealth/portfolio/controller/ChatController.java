package com.wealth.portfolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wealth.portfolio.dto.ChatRequest;
import com.wealth.portfolio.dto.ChatResponse;
import com.wealth.portfolio.mcp.PortfolioMcpTools;
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
    private final ClaudeService           claudeService;
    private final PortfolioMcpTools       mcpTools;

    public ChatController(PortfolioContextService portfolioContextService,
                          ClaudeService claudeService,
                          PortfolioMcpTools mcpTools) {
        this.portfolioContextService = portfolioContextService;
        this.claudeService           = claudeService;
        this.mcpTools                = mcpTools;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            String tab   = request.getTab() != null ? request.getTab() : "Portfolio Overview";
            String reply = switch (tab) {
                // Tool-enabled tabs: Claude fetches live data on demand via tool calling
                case "Portfolio Overview",
                     "Technical Analysis",
                     "Fundamentals"    -> claudeService.chatWithTools(toolSystemPrompt(tab), request.getMessage(), mcpTools);

                // Context-injected tabs: data comes from frontend localStorage
                case "Watchlist"      -> claudeService.chat(buildWatchlistPrompt(request.getTabContext()),     request.getMessage());
                case "Subscriptions"  -> claudeService.chat(buildSubscriptionsPrompt(request.getTabContext()), request.getMessage());

                default               -> claudeService.chatWithTools(toolSystemPrompt("Portfolio Overview"), request.getMessage(), mcpTools);
            };
            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Sorry, I encountered an error processing your request. Please try again."));
        }
    }

    // ── Tool-enabled system prompts (Claude fetches what it needs) ───────────────

    private String toolSystemPrompt(String tab) {
        return switch (tab) {
            case "Technical Analysis" -> """
                    You are a technical analysis assistant for the Wealth Dashboard.
                    Use get_technical_analysis to fetch support/resistance levels and RSI for any stock the user asks about.
                    Use get_stock_quote for a quick price check. Use search_stock if the user gives a company name instead of a ticker.
                    Be concise and actionable. Explain what the levels mean for the user's trading decisions.
                    """;
            case "Fundamentals" -> """
                    You are a fundamental analysis assistant for the Wealth Dashboard.
                    Use get_stock_fundamentals to retrieve metrics for any stock the user mentions.
                    Use search_stock if you need to find a ticker from a company name.
                    Compare metrics against benchmarks (PE < 15 good; ROE ≥ 15% good; etc.) and give clear investment insights.
                    """;
            default -> """
                    You are a personal wealth management assistant for the Wealth Dashboard.
                    Use get_portfolio_summary to retrieve the user's holdings before answering portfolio questions.
                    Use get_stock_quote or get_technical_analysis for market data questions.
                    Use get_stock_fundamentals for fundamental analysis. Use search_stock to find tickers by company name.
                    Be concise, accurate, and helpful.
                    """;
        };
    }

    // ── Context-injected prompts (data from frontend localStorage) ───────────────

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
        LocalDate today    = LocalDate.now();
        int dayOfMonth     = today.getDayOfMonth();
        LocalDate weekEnd  = today.plusDays(6);
        boolean sameMonth  = weekEnd.getMonthValue() == today.getMonthValue();
        int weekEndDay     = weekEnd.getDayOfMonth();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a personal finance assistant focused on the user's subscriptions and scheduled payments.\n");
        sb.append(String.format("Today is %s (day %d of the month, %s).%n",
                today, dayOfMonth, today.getDayOfWeek().toString().charAt(0)
                + today.getDayOfWeek().toString().substring(1).toLowerCase()));
        if (sameMonth) {
            sb.append(String.format("This week covers billing days %d – %d of this month.%n%n", dayOfMonth, weekEndDay));
        } else {
            sb.append(String.format("This week spans the end of this month (day %d+) and the start of next month (until day %d).%n%n",
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
}
