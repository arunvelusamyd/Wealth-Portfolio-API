package com.wealth.portfolio.controller;

import com.wealth.portfolio.dto.StockQuoteResponse;
import com.wealth.portfolio.dto.TickerSearchResult;
import com.wealth.portfolio.service.FinnhubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/search")
    public ResponseEntity<List<TickerSearchResult>> searchSymbols(@RequestParam String q) {
        if (q == null || q.trim().length() < 1) {
            return ResponseEntity.ok(List.of());
        }
        List<TickerSearchResult> results = finnhubService.searchSymbols(q.trim());
        return ResponseEntity.ok(results);
    }
}
