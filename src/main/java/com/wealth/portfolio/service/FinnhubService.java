package com.wealth.portfolio.service;

import com.wealth.portfolio.dto.StockQuoteResponse;
import com.wealth.portfolio.dto.TickerSearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinnhubService {

    private static final String FINNHUB_QUOTE_URL   = "https://finnhub.io/api/v1/quote";
    private static final String FINNHUB_SEARCH_URL  = "https://finnhub.io/api/v1/search";

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

    @SuppressWarnings("unchecked")
    public List<TickerSearchResult> searchSymbols(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(FINNHUB_SEARCH_URL)
                .queryParam("q", query)
                .queryParam("token", apiKey)
                .toUriString();

        Map<String, Object> raw = restTemplate.getForObject(url, Map.class);

        if (raw == null || !raw.containsKey("result")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) raw.get("result");

        return results.stream()
                .map(r -> new TickerSearchResult(
                        (String) r.getOrDefault("displaySymbol", ""),
                        (String) r.getOrDefault("description", ""),
                        (String) r.getOrDefault("type", "")
                ))
                .filter(r -> !r.getSymbol().isEmpty())
                .limit(10)
                .collect(Collectors.toList());
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
