package com.wealth.portfolio.dto;

import java.util.List;

public class StockHistoryResponse {

    private String       symbol;
    private String       period;
    private List<String> labels;
    private List<Double> prices;
    private double       firstPrice;
    private double       lastPrice;
    private double       changeAmount;
    private double       changePercent;

    public StockHistoryResponse() {}

    public StockHistoryResponse(String symbol, String period, List<String> labels, List<Double> prices,
                                 double firstPrice, double lastPrice, double changeAmount, double changePercent) {
        this.symbol        = symbol;
        this.period        = period;
        this.labels        = labels;
        this.prices        = prices;
        this.firstPrice    = firstPrice;
        this.lastPrice     = lastPrice;
        this.changeAmount  = changeAmount;
        this.changePercent = changePercent;
    }

    public String       getSymbol()        { return symbol; }
    public String       getPeriod()        { return period; }
    public List<String> getLabels()        { return labels; }
    public List<Double> getPrices()        { return prices; }
    public double       getFirstPrice()    { return firstPrice; }
    public double       getLastPrice()     { return lastPrice; }
    public double       getChangeAmount()  { return changeAmount; }
    public double       getChangePercent() { return changePercent; }

    public void setSymbol(String symbol)               { this.symbol = symbol; }
    public void setPeriod(String period)               { this.period = period; }
    public void setLabels(List<String> labels)         { this.labels = labels; }
    public void setPrices(List<Double> prices)         { this.prices = prices; }
    public void setFirstPrice(double firstPrice)       { this.firstPrice = firstPrice; }
    public void setLastPrice(double lastPrice)         { this.lastPrice = lastPrice; }
    public void setChangeAmount(double changeAmount)   { this.changeAmount = changeAmount; }
    public void setChangePercent(double changePercent) { this.changePercent = changePercent; }
}
