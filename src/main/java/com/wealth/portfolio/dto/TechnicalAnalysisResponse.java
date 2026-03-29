package com.wealth.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class TechnicalAnalysisResponse {

    private String symbol;
    private Double currentPrice;
    private Double change;
    private Double percentChange;
    private Double high;
    private Double low;
    private Double open;
    private Double previousClose;
    private List<Double> supportLevels;
    private List<Double> resistanceLevels;
    private Double rsi;
    private String rsiSignal;
    private String resolution;

    public TechnicalAnalysisResponse() {}

    public TechnicalAnalysisResponse(String symbol, Double currentPrice, Double change,
                                     Double percentChange, Double high, Double low,
                                     Double open, Double previousClose,
                                     List<Double> supportLevels, List<Double> resistanceLevels,
                                     Double rsi, String rsiSignal, String resolution) {
        this.symbol           = symbol;
        this.currentPrice     = currentPrice;
        this.change           = change;
        this.percentChange    = percentChange;
        this.high             = high;
        this.low              = low;
        this.open             = open;
        this.previousClose    = previousClose;
        this.supportLevels    = supportLevels;
        this.resistanceLevels = resistanceLevels;
        this.rsi              = rsi;
        this.rsiSignal        = rsiSignal;
        this.resolution       = resolution;
    }

    public String   getSymbol()           { return symbol; }
    public Double   getCurrentPrice()     { return currentPrice; }
    public Double   getChange()           { return change; }
    public Double   getPercentChange()    { return percentChange; }
    public Double   getHigh()             { return high; }
    public Double   getLow()              { return low; }
    public Double   getOpen()             { return open; }
    public Double   getPreviousClose()    { return previousClose; }
    public List<Double> getSupportLevels()    { return supportLevels; }
    public List<Double> getResistanceLevels() { return resistanceLevels; }
    public Double   getRsi()              { return rsi; }
    public String   getRsiSignal()        { return rsiSignal; }
    public String   getResolution()       { return resolution; }
}
