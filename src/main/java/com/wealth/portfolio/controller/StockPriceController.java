package com.wealth.portfolio.controller;

import com.wealth.portfolio.dto.StockQuoteResponse;
import com.wealth.portfolio.service.FinnhubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockPriceController {

    private final FinnhubService finnhubService;

    public StockPriceController(FinnhubService finnhubService) {
        this.finnhubService = finnhubService;
    }

    @GetMapping("/{symbol}/price")
    public ResponseEntity<StockQuoteResponse> getStockPrice(@PathVariable String symbol) {
        StockQuoteResponse quote = finnhubService.getStockQuote(symbol);
        return ResponseEntity.ok(quote);
    }
}
