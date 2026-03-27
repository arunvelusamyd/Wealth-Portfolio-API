package com.wealth.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PortfolioContextService {

    private static final String[] BANK_CODES = {"SC", "UOB", "OCBC", "DBS"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String buildPortfolioContext() throws IOException {
        StringBuilder context = new StringBuilder();
        context.append(buildStocksContext());
        context.append("\n\n");
        context.append(buildUnitTrustsContext());
        return context.toString();
    }

    private String buildStocksContext() {
        StringBuilder sb = new StringBuilder("## Stocks & ETF Portfolio\n");

        for (String bankCode : BANK_CODES) {
            String path = "BankResponse/" + bankCode + "/Stocks.json";
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) continue;

            try {
                JsonNode root = objectMapper.readTree(resource.getInputStream());
                JsonNode stocks = root.has("data") ? root.get("data") : root;

                for (JsonNode item : stocks) {
                    String code = item.has("code") ? item.get("code").asText("") : "";
                    if (code.startsWith("total-") || code.startsWith("gtotal-") || code.startsWith("ototal-")) continue;
                    if (code.isBlank()) continue;

                    String name   = item.has("name")   ? item.get("name").asText(code)  : code;
                    double qty    = item.has("qty")     ? item.get("qty").asDouble(0)    : 0;
                    double wac    = item.has("wac")     ? item.get("wac").asDouble(0)    : 0;
                    double mktval = item.has("mktval")  ? item.get("mktval").asDouble(0) : 0;
                    double profit = item.has("profit")  ? item.get("profit").asDouble(0) : 0;
                    String ccy    = item.has("ccy")     ? item.get("ccy").asText("")     : "";
                    String mkt    = item.has("mkt")     ? item.get("mkt").asText("")     : "";

                    sb.append(String.format("- %s (%s) [%s]: %s units, WAC %.2f, Market Value %.2f %s, Unrealised P&L %.2f %s%n",
                            name, code, mkt, formatQty(qty), wac, mktval, ccy, profit, ccy));
                }
            } catch (IOException ignored) {
            }
        }

        return sb.toString();
    }

    private String buildUnitTrustsContext() {
        StringBuilder sb = new StringBuilder("## Unit Trust / Mutual Fund Portfolio\n");

        for (String bankCode : BANK_CODES) {
            // Try standard naming first, then DBS-style hyphenated
            ClassPathResource resource = new ClassPathResource("BankResponse/" + bankCode + "/UnitTrust.json");
            if (!resource.exists()) {
                resource = new ClassPathResource("BankResponse/" + bankCode + "/unit-trusts.json");
            }
            if (!resource.exists()) continue;

            try {
                JsonNode root = objectMapper.readTree(resource.getInputStream());

                // DBS format: { "investment": { "accounts": [...] } }
                if (root.has("investment") && root.get("investment").has("accounts")) {
                    JsonNode accounts = root.get("investment").get("accounts");
                    for (JsonNode acct : accounts) {
                        double mktVal = acct.has("marketValue")
                                ? acct.get("marketValue").path("displayBalance").asDouble(0) : 0;
                        if (mktVal <= 0) continue;
                        String fundName = acct.has("productCodeDescription")
                                ? acct.get("productCodeDescription").asText("") : "";
                        if (fundName.isBlank() && acct.has("accountNickname"))
                            fundName = acct.get("accountNickname").asText("Unit Trust");
                        String fundCode = acct.has("investmentId") ? acct.get("investmentId").asText("") : "";
                        sb.append(String.format("- %s (%s): Market Value SGD %.2f%n", fundName, fundCode, mktVal));
                    }
                    continue;
                }

                // Standard format: array of unit trust objects
                for (JsonNode item : root) {
                    String fundName = item.has("fundName")  ? item.get("fundName").asText("")  : "";
                    String fundCode = item.has("fundCode")  ? item.get("fundCode").asText("")  : "";
                    double units    = item.has("currentUnits")           ? item.get("currentUnits").asDouble(0)           : 0;
                    double nav      = item.has("nav")                    ? item.get("nav").asDouble(0)                    : 0;
                    double mktVal   = item.has("marketValueBaseCcy")     ? item.get("marketValueBaseCcy").asDouble(0)     : 0;
                    double pl       = item.has("unRealPLInBaseCcy")      ? item.get("unRealPLInBaseCcy").asDouble(0)      : 0;
                    String divInstr = item.has("dividendInstruction")    ? item.get("dividendInstruction").asText("")     : "";
                    String divMode  = "R".equals(divInstr) ? "Reinvest" : "C".equals(divInstr) ? "Cash" : divInstr;

                    sb.append(String.format("- %s (%s): %.4f units, NAV %.4f, Market Value SGD %.2f, Unrealised P&L SGD %.2f, Dividend: %s%n",
                            fundName, fundCode, units, nav, mktVal, pl, divMode));
                }
            } catch (IOException ignored) {
            }
        }

        return sb.toString();
    }

    private String formatQty(double qty) {
        if (qty == Math.floor(qty)) return String.valueOf((long) qty);
        return String.format("%.4f", qty);
    }
}
