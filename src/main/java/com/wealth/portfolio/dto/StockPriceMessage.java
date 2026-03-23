package com.wealth.portfolio.dto;

public class StockPriceMessage {

    private String symbol;
    private double price;
    private long timestamp;
    private double volume;

    public StockPriceMessage() {}

    public StockPriceMessage(String symbol, double price, long timestamp, double volume) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
        this.volume = volume;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
}
