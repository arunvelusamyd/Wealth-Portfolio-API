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

    @GetMapping(value = "/cash", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getCash() throws IOException {
        InputStream inputStream = new ClassPathResource("Cash.json").getInputStream();
        JsonNode json = objectMapper.readTree(inputStream);
        return ResponseEntity.ok(json);
    }

    @GetMapping(value = "/performance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getPerformance() throws IOException {
        InputStream inputStream = new ClassPathResource("Performance.json").getInputStream();
        JsonNode json = objectMapper.readTree(inputStream);
        return ResponseEntity.ok(json);
    }

    @GetMapping(value = "/cpf", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getCpf() throws IOException {
        InputStream inputStream = new ClassPathResource("Cpf.json").getInputStream();
        JsonNode json = objectMapper.readTree(inputStream);
        return ResponseEntity.ok(json);
    }

    @GetMapping(value = "/srs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getSrs() throws IOException {
        InputStream inputStream = new ClassPathResource("Srs.json").getInputStream();
        JsonNode json = objectMapper.readTree(inputStream);
        return ResponseEntity.ok(json);
    }

    @GetMapping(value = "/news", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getNews() throws IOException {
        InputStream inputStream = new ClassPathResource("News.json").getInputStream();
        JsonNode json = objectMapper.readTree(inputStream);
        return ResponseEntity.ok(json);
    }
}
