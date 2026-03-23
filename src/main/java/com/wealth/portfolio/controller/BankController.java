package com.wealth.portfolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/banks")
public class BankController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getBanks() throws IOException {
        ClassPathResource resource = new ClassPathResource("Banks.json");
        JsonNode json = objectMapper.readTree(resource.getInputStream());
        return ResponseEntity.ok(json);
    }

    @GetMapping(value = "/{bankCode}/portfolio", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectNode> getBankPortfolio(@PathVariable String bankCode) {
        String code = bankCode.toUpperCase();
        ObjectNode portfolio = objectMapper.createObjectNode();
        portfolio.put("bankCode", code);

        setIfExists(portfolio,     "BankResponse/" + code + "/Stocks.json",          "stocks");
        // unit trusts: try both naming conventions (SC/UOB/OCBC use UnitTrust.json, DBS uses unit-trusts.json)
        setFirstExists(portfolio,  "unitTrusts",
                "BankResponse/" + code + "/UnitTrust.json",
                "BankResponse/" + code + "/unit-trusts.json");
        setIfExists(portfolio,     "BankResponse/" + code + "/Cash.json",            "cash");
        setIfExists(portfolio,     "BankResponse/" + code + "/CASA.json",            "casa");
        setIfExists(portfolio,     "BankResponse/" + code + "/Fixed-Deposits.json",  "fixedDeposits");
        setIfExists(portfolio,     "BankResponse/" + code + "/Cpf.json",             "cpf");
        setIfExists(portfolio,     "BankResponse/" + code + "/Srs.json",             "srs");
        setIfExists(portfolio,     "BankResponse/" + code + "/SRS-Unit-Trusts.json", "srsUnitTrusts");
        setIfExists(portfolio,     "BankResponse/" + code + "/Performance.json",     "performance");
        setIfExists(portfolio,     "BankResponse/" + code + "/News.json",            "news");

        return ResponseEntity.ok(portfolio);
    }

    /** Sets the first file path that exists on the classpath; sets null if none match. */
    private void setFirstExists(ObjectNode node, String key, String... paths) {
        for (String path : paths) {
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (resource.exists()) {
                    node.set(key, objectMapper.readTree(resource.getInputStream()));
                    return;
                }
            } catch (IOException ignored) {
            }
        }
        node.putNull(key);
    }

    private void setIfExists(ObjectNode node, String path, String key) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                node.set(key, objectMapper.readTree(resource.getInputStream()));
            } else {
                node.putNull(key);
            }
        } catch (IOException e) {
            node.putNull(key);
        }
    }
}
