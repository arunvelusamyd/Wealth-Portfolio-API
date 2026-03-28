package com.wealth.portfolio.dto;

public class TickerSearchResult {

    private final String symbol;
    private final String description;
    private final String type;

    public TickerSearchResult(String symbol, String description, String type) {
        this.symbol = symbol;
        this.description = description;
        this.type = type;
    }

    public String getSymbol()      { return symbol; }
    public String getDescription() { return description; }
    public String getType()        { return type; }
}
