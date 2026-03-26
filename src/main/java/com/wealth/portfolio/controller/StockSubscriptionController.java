package com.wealth.portfolio.controller;

import com.wealth.portfolio.dto.SubscribeRequest;
import com.wealth.portfolio.service.FinnhubStreamingService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class StockSubscriptionController {

    private final FinnhubStreamingService finnhubStreamingService;

    public StockSubscriptionController(FinnhubStreamingService finnhubStreamingService) {
        this.finnhubStreamingService = finnhubStreamingService;
    }

    @MessageMapping("/stocks/subscribe")
    public void subscribe(SubscribeRequest request) {
        if (request.getSymbols() != null) {
            request.getSymbols().forEach(finnhubStreamingService::subscribe);
        }
    }

    @MessageMapping("/stocks/unsubscribe")
    public void unsubscribe(SubscribeRequest request) {
        if (request.getSymbols() != null) {
            request.getSymbols().forEach(finnhubStreamingService::unsubscribe);
        }
    }
}
