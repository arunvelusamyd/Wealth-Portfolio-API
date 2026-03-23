package com.wealth.portfolio.dto;

import java.util.List;

public class SubscribeRequest {

    private List<String> symbols;

    public List<String> getSymbols() { return symbols; }
    public void setSymbols(List<String> symbols) { this.symbols = symbols; }
}
