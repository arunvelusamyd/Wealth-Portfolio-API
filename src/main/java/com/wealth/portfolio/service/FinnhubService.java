package com.wealth.portfolio.service;

import com.wealth.portfolio.dto.StockFundamentalsResponse;
import com.wealth.portfolio.dto.StockQuoteResponse;
import com.wealth.portfolio.dto.TechnicalAnalysisResponse;
import com.wealth.portfolio.dto.TickerSearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinnhubService {

    private static final String FINNHUB_QUOTE_URL     = "https://finnhub.io/api/v1/quote";
    private static final String FINNHUB_SEARCH_URL    = "https://finnhub.io/api/v1/search";
    private static final String FINNHUB_METRICS_URL   = "https://finnhub.io/api/v1/stock/metric";
    private static final String FINNHUB_SR_URL        = "https://finnhub.io/api/v1/scan/support-resistance";
    private static final String FINNHUB_INDICATOR_URL = "https://finnhub.io/api/v1/indicator";

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

    @SuppressWarnings("unchecked")
    public TechnicalAnalysisResponse getTechnicalAnalysis(String symbol, String resolution) {
        String res = (resolution != null && !resolution.isBlank()) ? resolution.toUpperCase() : "D";

        // 1. Current quote
        StockQuoteResponse quote = getStockQuote(symbol);
        double currentPrice = quote.getCurrentPrice();

        // 2. Support & resistance levels
        List<Double> supportLevels    = new ArrayList<>();
        List<Double> resistanceLevels = new ArrayList<>();
        try {
            String srUrl = UriComponentsBuilder.fromHttpUrl(FINNHUB_SR_URL)
                    .queryParam("symbol", symbol.toUpperCase())
                    .queryParam("resolution", res)
                    .queryParam("token", apiKey)
                    .toUriString();
            Map<String, Object> srRaw = restTemplate.getForObject(srUrl, Map.class);
            if (srRaw != null && srRaw.containsKey("levels")) {
                List<?> levels = (List<?>) srRaw.get("levels");
                for (Object level : levels) {
                    if (level instanceof Number) {
                        double l = ((Number) level).doubleValue();
                        if (l < currentPrice)      supportLevels.add(l);
                        else if (l > currentPrice) resistanceLevels.add(l);
                    }
                }
                // Nearest level first
                supportLevels.sort(Comparator.reverseOrder());
                resistanceLevels.sort(Comparator.naturalOrder());
            }
        } catch (Exception ignored) {}

        // 3. RSI (14-period, last 90 days of daily data)
        Double rsi       = null;
        String rsiSignal = null;
        try {
            long toTime   = Instant.now().getEpochSecond();
            long fromTime = toTime - (90L * 24 * 3600);
            String rsiUrl = UriComponentsBuilder.fromHttpUrl(FINNHUB_INDICATOR_URL)
                    .queryParam("symbol", symbol.toUpperCase())
                    .queryParam("resolution", res)
                    .queryParam("from", fromTime)
                    .queryParam("to", toTime)
                    .queryParam("indicator", "rsi")
                    .queryParam("timeperiod", 14)
                    .queryParam("token", apiKey)
                    .toUriString();
            Map<String, Object> rsiRaw = restTemplate.getForObject(rsiUrl, Map.class);
            if (rsiRaw != null && "ok".equals(rsiRaw.get("s"))) {
                List<?> rsiValues = (List<?>) rsiRaw.get("rsi");
                if (rsiValues != null && !rsiValues.isEmpty()) {
                    Object last = rsiValues.get(rsiValues.size() - 1);
                    if (last instanceof Number) {
                        rsi = ((Number) last).doubleValue();
                        if      (rsi < 30) rsiSignal = "Oversold";
                        else if (rsi > 70) rsiSignal = "Overbought";
                        else               rsiSignal = "Neutral";
                    }
                }
            }
        } catch (Exception ignored) {}

        return new TechnicalAnalysisResponse(
                symbol.toUpperCase(),
                currentPrice,
                quote.getChange(),
                quote.getPercentChange(),
                quote.getHigh(),
                quote.getLow(),
                quote.getOpen(),
                quote.getPreviousClose(),
                supportLevels,
                resistanceLevels,
                rsi,
                rsiSignal,
                res
        );
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
