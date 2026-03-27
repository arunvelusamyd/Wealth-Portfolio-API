package com.wealth.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class StockFundamentalsResponse {

    private String symbol;

    /** Price-to-Earnings ratio (normalised, annual) */
    private Double peRatio;

    /** Price-to-Book ratio (tangible book, annual) */
    private Double pbRatio;

    /** Indicated annual dividend yield (%) */
    private Double dividendYield;

    /** Net profit margin – Net Income / Sales (%, TTM) */
    private Double netProfitMargin;

    /** Earnings / Book Value – EPS ÷ Book Value per share (annual) */
    private Double earningsToBookValue;

    /** PEG ratio – P/E ÷ 5-year EPS growth rate */
    private Double pegRatio;

    /** Current ratio – Current Assets ÷ Current Liabilities (annual) */
    private Double currentRatio;

    /** Working Capital / Debt – requires absolute balance-sheet data; null when unavailable */
    private Double workingCapitalToDebt;

    /** Net Current Asset / Debt (Graham metric) – null when unavailable */
    private Double netCurrentAssetToDebt;

    /** Return on Equity (%, TTM) */
    private Double roe;

    /** Return on Invested Capital used as ROCE proxy (%, TTM) */
    private Double roce;

    public StockFundamentalsResponse() {}

    public StockFundamentalsResponse(String symbol, Double peRatio, Double pbRatio,
                                     Double dividendYield, Double netProfitMargin,
                                     Double earningsToBookValue, Double pegRatio,
                                     Double currentRatio, Double workingCapitalToDebt,
                                     Double netCurrentAssetToDebt, Double roe, Double roce) {
        this.symbol = symbol;
        this.peRatio = peRatio;
        this.pbRatio = pbRatio;
        this.dividendYield = dividendYield;
        this.netProfitMargin = netProfitMargin;
        this.earningsToBookValue = earningsToBookValue;
        this.pegRatio = pegRatio;
        this.currentRatio = currentRatio;
        this.workingCapitalToDebt = workingCapitalToDebt;
        this.netCurrentAssetToDebt = netCurrentAssetToDebt;
        this.roe = roe;
        this.roce = roce;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Double getPeRatio() { return peRatio; }
    public void setPeRatio(Double peRatio) { this.peRatio = peRatio; }

    public Double getPbRatio() { return pbRatio; }
    public void setPbRatio(Double pbRatio) { this.pbRatio = pbRatio; }

    public Double getDividendYield() { return dividendYield; }
    public void setDividendYield(Double dividendYield) { this.dividendYield = dividendYield; }

    public Double getNetProfitMargin() { return netProfitMargin; }
    public void setNetProfitMargin(Double netProfitMargin) { this.netProfitMargin = netProfitMargin; }

    public Double getEarningsToBookValue() { return earningsToBookValue; }
    public void setEarningsToBookValue(Double earningsToBookValue) { this.earningsToBookValue = earningsToBookValue; }

    public Double getPegRatio() { return pegRatio; }
    public void setPegRatio(Double pegRatio) { this.pegRatio = pegRatio; }

    public Double getCurrentRatio() { return currentRatio; }
    public void setCurrentRatio(Double currentRatio) { this.currentRatio = currentRatio; }

    public Double getWorkingCapitalToDebt() { return workingCapitalToDebt; }
    public void setWorkingCapitalToDebt(Double workingCapitalToDebt) { this.workingCapitalToDebt = workingCapitalToDebt; }

    public Double getNetCurrentAssetToDebt() { return netCurrentAssetToDebt; }
    public void setNetCurrentAssetToDebt(Double netCurrentAssetToDebt) { this.netCurrentAssetToDebt = netCurrentAssetToDebt; }

    public Double getRoe() { return roe; }
    public void setRoe(Double roe) { this.roe = roe; }

    public Double getRoce() { return roce; }
    public void setRoce(Double roce) { this.roce = roce; }
}
