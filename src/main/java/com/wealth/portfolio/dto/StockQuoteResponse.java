package com.wealth.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StockQuoteResponse {

    private String symbol;

    @JsonProperty("currentPrice")
    private double currentPrice;

    @JsonProperty("change")
    private double change;

    @JsonProperty("percentChange")
    private double percentChange;

    @JsonProperty("high")
    private double high;

    @JsonProperty("low")
    private double low;

    @JsonProperty("open")
    private double open;

    @JsonProperty("previousClose")
    private double previousClose;

    @JsonProperty("timestamp")
    private long timestamp;

    public StockQuoteResponse() {}

    public StockQuoteResponse(String symbol, double currentPrice, double change, double percentChange,
                               double high, double low, double open, double previousClose, long timestamp) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.change = change;
        this.percentChange = percentChange;
        this.high = high;
        this.low = low;
        this.open = open;
        this.previousClose = previousClose;
        this.timestamp = timestamp;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }

    public double getPercentChange() { return percentChange; }
    public void setPercentChange(double percentChange) { this.percentChange = percentChange; }

    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }

    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }

    public double getOpen() { return open; }
    public void setOpen(double open) { this.open = open; }

    public double getPreviousClose() { return previousClose; }
    public void setPreviousClose(double previousClose) { this.previousClose = previousClose; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
