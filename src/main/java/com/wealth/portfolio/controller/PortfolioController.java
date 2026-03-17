package com.wealth.portfolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/stocks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getStocks() throws IOException {
        InputStream inputStream = new ClassPathResource("Stocks.json").getInputStream();
        JsonNode json = objectMapper.readTree(inputStream);
        return ResponseEntity.ok(json);
    }

    @GetMapping(value = "/unit-trust", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getUnitTrust() throws IOException {
        InputStream inputStream = new ClassPathResource("UnitTrust.json").getInputStream();
        JsonNode json = objectMapper.readTree(inputStream);
        return ResponseEntity.ok(json);
    }
}
