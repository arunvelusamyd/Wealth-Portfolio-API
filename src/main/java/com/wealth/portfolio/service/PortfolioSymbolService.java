package com.wealth.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Service
public class PortfolioSymbolService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioSymbolService.class);
    private static final String[] BANK_CODES = {"SC", "OCBC", "UOB", "DBS"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Set<String> getAllStockSymbols() {
        Set<String> symbols = new HashSet<>();
        extractSymbols("Stocks.json", symbols);
        for (String bank : BANK_CODES) {
            extractSymbols("BankResponse/" + bank + "/Stocks.json", symbols);
        }
        log.info("Loaded {} unique stock symbols from portfolio", symbols.size());
        return symbols;
    }

    private void extractSymbols(String path, Set<String> symbols) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) return;

        try {
            JsonNode root = objectMapper.readTree(resource.getInputStream());
            JsonNode stocks = root.has("data") ? root.get("data") : root;

            for (JsonNode item : stocks) {
                String code = item.path("code").asText("").trim();
                if (!code.isBlank()
                        && !code.startsWith("total-")
                        && !code.startsWith("gtotal-")
                        && !code.startsWith("ototal-")) {
                    symbols.add(code);
                }
            }
        } catch (IOException e) {
            log.warn("Could not read symbols from {}: {}", path, e.getMessage());
        }
    }
}
