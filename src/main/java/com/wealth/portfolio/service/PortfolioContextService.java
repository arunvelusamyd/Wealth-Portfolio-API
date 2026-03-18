package com.wealth.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class PortfolioContextService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String buildPortfolioContext() throws IOException {
        StringBuilder context = new StringBuilder();
        context.append(buildStocksContext());
        context.append("\n\n");
        context.append(buildUnitTrustsContext());
        return context.toString();
    }

    private String buildStocksContext() throws IOException {
        InputStream inputStream = new ClassPathResource("Stocks.json").getInputStream();
        JsonNode root = objectMapper.readTree(inputStream);

        JsonNode stocks = root.has("data") ? root.get("data") : root;

        StringBuilder sb = new StringBuilder();
        sb.append("## Stocks & ETF Portfolio\n");

        for (JsonNode item : stocks) {
            // Skip subtotal/grand total rows
            String code = item.has("code") ? item.get("code").asText("") : "";
            if (code.startsWith("total-") || code.startsWith("gtotal-") || code.startsWith("ototal-")) {
                continue;
            }
            if (code.isBlank()) continue;

            String name = item.has("name") ? item.get("name").asText("") : code;
            double qty = item.has("qty") ? item.get("qty").asDouble(0) : 0;
            double wac = item.has("wac") ? item.get("wac").asDouble(0) : 0;
            double mktval = item.has("mktval") ? item.get("mktval").asDouble(0) : 0;
            double profit = item.has("profit") ? item.get("profit").asDouble(0) : 0;
            String ccy = item.has("ccy") ? item.get("ccy").asText("") : "";
            String mkt = item.has("mkt") ? item.get("mkt").asText("") : "";

            sb.append(String.format("- %s (%s) [%s]: %s units, WAC %.2f, Market Value %.2f %s, Unrealised P&L %.2f %s%n",
                    name, code, mkt, formatQty(qty), wac, mktval, ccy, profit, ccy));
        }

        return sb.toString();
    }

    private String buildUnitTrustsContext() throws IOException {
        InputStream inputStream = new ClassPathResource("UnitTrust.json").getInputStream();
        JsonNode unitTrusts = objectMapper.readTree(inputStream);

        StringBuilder sb = new StringBuilder();
        sb.append("## Unit Trust / Mutual Fund Portfolio\n");

        for (JsonNode item : unitTrusts) {
            String fundName = item.has("fundName") ? item.get("fundName").asText("") : "";
            String fundCode = item.has("fundCode") ? item.get("fundCode").asText("") : "";
            double units = item.has("currentUnits") ? item.get("currentUnits").asDouble(0) : 0;
            double nav = item.has("nav") ? item.get("nav").asDouble(0) : 0;
            double mktVal = item.has("marketValueBaseCcy") ? item.get("marketValueBaseCcy").asDouble(0) : 0;
            double plBaseCcy = item.has("unRealPLInBaseCcy") ? item.get("unRealPLInBaseCcy").asDouble(0) : 0;
            String divInstruction = item.has("dividendInstruction") ? item.get("dividendInstruction").asText("") : "";
            String divMode = "R".equals(divInstruction) ? "Reinvest" : "C".equals(divInstruction) ? "Cash" : divInstruction;

            sb.append(String.format("- %s (%s): %.4f units, NAV %.4f, Market Value SGD %.2f, Unrealised P&L SGD %.2f, Dividend: %s%n",
                    fundName, fundCode, units, nav, mktVal, plBaseCcy, divMode));
        }

        return sb.toString();
    }

    private String formatQty(double qty) {
        if (qty == Math.floor(qty)) {
            return String.valueOf((long) qty);
        }
        return String.format("%.4f", qty);
    }
}
