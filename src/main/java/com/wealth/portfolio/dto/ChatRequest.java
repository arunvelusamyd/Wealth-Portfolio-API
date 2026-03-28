package com.wealth.portfolio.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class ChatRequest {

    private String message;
    private String tab;
    private JsonNode tabContext;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTab() { return tab; }
    public void setTab(String tab) { this.tab = tab; }

    public JsonNode getTabContext() { return tabContext; }
    public void setTabContext(JsonNode tabContext) { this.tabContext = tabContext; }
}
