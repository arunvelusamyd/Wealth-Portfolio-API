package com.wealth.portfolio.service;

import com.wealth.portfolio.dto.StockQuoteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class FinnhubService {

    private static final String FINNHUB_QUOTE_URL = "https://finnhub.io/api/v1/quote";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public FinnhubService(RestTemplate restTemplate,
                          @Value("${finnhub.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    public StockQuoteResponse getStockQuote(String symbol) {
        String url = UriComponentsBuilder.fromHttpUrl(FINNHUB_QUOTE_URL)
                .queryParam("symbol", symbol.toUpperCase())
                .queryParam("token", apiKey)
                .toUriString();

        @SuppressWarnings("unchecked")
        Map<String, Object> raw = restTemplate.getForObject(url, Map.class);

        if (raw == null || raw.get("c") == null) {
            throw new IllegalArgumentException("No data found for symbol: " + symbol);
        }

        return new StockQuoteResponse(
                symbol.toUpperCase(),
                toDouble(raw.get("c")),   // current price
                toDouble(raw.get("d")),   // change
                toDouble(raw.get("dp")),  // percent change
                toDouble(raw.get("h")),   // high
                toDouble(raw.get("l")),   // low
                toDouble(raw.get("o")),   // open
                toDouble(raw.get("pc")),  // previous close
                toLong(raw.get("t"))      // timestamp
        );
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    private long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
}
