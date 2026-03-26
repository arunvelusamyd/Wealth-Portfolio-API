package com.wealth.portfolio.service;

import com.wealth.portfolio.dto.StockFundamentalsResponse;
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
    private static final String FINNHUB_METRICS_URL = "https://finnhub.io/api/v1/stock/metric";

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

    @SuppressWarnings("unchecked")
    public StockFundamentalsResponse getFundamentals(String symbol) {
        String url = UriComponentsBuilder.fromHttpUrl(FINNHUB_METRICS_URL)
                .queryParam("symbol", symbol.toUpperCase())
                .queryParam("metric", "all")
                .queryParam("token", apiKey)
                .toUriString();

        Map<String, Object> raw = restTemplate.getForObject(url, Map.class);
        if (raw == null || !raw.containsKey("metric")) {
            return new StockFundamentalsResponse(symbol.toUpperCase(),
                    null, null, null, null, null, null, null, null, null, null, null);
        }

        Map<String, Object> m = (Map<String, Object>) raw.get("metric");

        Double pe    = metricDouble(m, "peNormalizedAnnual");
        Double pb    = metricDouble(m, "ptbvAnnual");
        Double divY  = metricDouble(m, "dividendYieldIndicatedAnnual");
        Double npm   = metricDouble(m, "netMarginTTM");

        Double eps   = metricDouble(m, "epsAnnual");
        Double bvps  = metricDouble(m, "bookValuePerShareAnnual");
        Double ebv   = (eps != null && bvps != null && bvps != 0) ? eps / bvps : null;

        Double epsGrowth = metricDouble(m, "epsGrowth5Y");
        Double peg   = (pe != null && epsGrowth != null && epsGrowth > 0) ? pe / epsGrowth : null;

        Double cr    = metricDouble(m, "currentRatioAnnual");
        Double roe   = metricDouble(m, "roeTTM");
        Double roce  = metricDouble(m, "roicTTM");

        return new StockFundamentalsResponse(symbol.toUpperCase(),
                pe, pb, divY, npm, ebv, peg, cr, null, null, roe, roce);
    }

    private Double metricDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return null;
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
