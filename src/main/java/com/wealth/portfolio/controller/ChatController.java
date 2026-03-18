package com.wealth.portfolio.controller;

import com.wealth.portfolio.dto.ChatRequest;
import com.wealth.portfolio.dto.ChatResponse;
import com.wealth.portfolio.service.ClaudeService;
import com.wealth.portfolio.service.PortfolioContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            String portfolioContext = portfolioContextService.buildPortfolioContext();

            String systemPrompt = """
                    You are a personal wealth management assistant. You have access to the user's
                    current investment portfolio listed below. Answer questions accurately using only
                    the data provided. Be concise and helpful. If a question cannot be answered from
                    the portfolio data alone, say so clearly rather than guessing.

                    """ + portfolioContext;

            String reply = claudeService.chat(systemPrompt, request.getMessage());
            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Sorry, I encountered an error processing your request. Please try again."));
        }
    }
}
