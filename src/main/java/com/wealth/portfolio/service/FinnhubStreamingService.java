package com.wealth.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealth.portfolio.dto.StockPriceMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FinnhubStreamingService {

    private static final Logger log = LoggerFactory.getLogger(FinnhubStreamingService.class);
    private static final String FINNHUB_WS_URL = "wss://ws.finnhub.io?token=";

    private final SimpMessagingTemplate messagingTemplate;
    private final PortfolioSymbolService portfolioSymbolService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private WebSocketClient wsClient;

    public FinnhubStreamingService(SimpMessagingTemplate messagingTemplate,
                                   PortfolioSymbolService portfolioSymbolService,
                                   @Value("${finnhub.api.key}") String apiKey) {
        this.messagingTemplate = messagingTemplate;
        this.portfolioSymbolService = portfolioSymbolService;
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void init() {
        subscribedSymbols.addAll(portfolioSymbolService.getAllStockSymbols());
        connect();
    }

    private void connect() {
        try {
            wsClient = new WebSocketClient(new URI(FINNHUB_WS_URL + apiKey)) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    log.info("Connected to Finnhub WebSocket — subscribing {} symbols", subscribedSymbols.size());
                    subscribedSymbols.forEach(FinnhubStreamingService.this::sendSubscribe);
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("Finnhub WebSocket closed [code={}, reason={}, remote={}]", code, reason, remote);
                    if (remote) scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("Finnhub WebSocket error", ex);
                }
            };
            wsClient.connect();
        } catch (URISyntaxException e) {
            log.error("Invalid Finnhub WebSocket URI", e);
        }
    }

    public void subscribe(String symbol) {
        subscribedSymbols.add(symbol);
        if (wsClient != null && wsClient.isOpen()) {
            sendSubscribe(symbol);
        }
    }

    public void unsubscribe(String symbol) {
        subscribedSymbols.remove(symbol);
        if (wsClient != null && wsClient.isOpen()) {
            sendUnsubscribe(symbol);
        }
    }

    private void sendSubscribe(String symbol) {
        send(Map.of("type", "subscribe", "symbol", symbol));
    }

    private void sendUnsubscribe(String symbol) {
        send(Map.of("type", "unsubscribe", "symbol", symbol));
    }

    private void send(Object payload) {
        try {
            wsClient.send(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WS message", e);
        }
    }

    private void handleMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (!"trade".equals(root.path("type").asText())) return;

            for (JsonNode trade : root.path("data")) {
                String symbol = trade.path("s").asText();
                double price = trade.path("p").asDouble();
                long timestamp = trade.path("t").asLong();
                double volume = trade.path("v").asDouble();

                StockPriceMessage priceMsg = new StockPriceMessage(symbol, price, timestamp, volume);
                messagingTemplate.convertAndSend("/topic/prices/" + symbol, priceMsg);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Finnhub message: {}", message, e);
        }
    }

    private void scheduleReconnect() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                log.info("Reconnecting to Finnhub WebSocket...");
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void disconnect() {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }
}
