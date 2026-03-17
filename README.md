# Wealth Portfolio API

A Spring Boot REST API that serves portfolio data for **Stocks** and **Unit Trusts**.

---

## Tech Stack

| Component      | Version |
|----------------|---------|
| Java           | 17      |
| Spring Boot    | 3.3.4   |
| Maven          | 3.x     |
| Embedded Server| Tomcat (port 8080) |

---

## Project Structure

```
Wealth-Portfolio-API/
├── pom.xml
├── Stocks.json                          # Source data (root)
├── UnitTrust.json                       # Source data (root)
└── src/
    └── main/
        ├── java/com/wealth/portfolio/
        │   ├── WealthPortfolioApplication.java   # Entry point
        │   └── controller/
        │       └── PortfolioController.java      # REST endpoints
        └── resources/
            ├── application.properties
            ├── Stocks.json                       # Classpath copy
            └── UnitTrust.json                    # Classpath copy
```

---

## Prerequisites

- Java 17+
- Maven 3.6+

Verify:
```bash
java -version
mvn -version
```

---

## Local Setup & Run

**1. Clone / navigate to the project root:**
```bash
cd Wealth-Portfolio-API
```

**2. Build the project:**
```bash
mvn clean package
```

**3. Run the application:**
```bash
java -jar target/wealth-portfolio-api-1.0.0.jar
```

The server starts on **http://localhost:8080**

---

## API Reference

### 1. Get Stocks Portfolio

Returns the full stocks portfolio data including individual holdings, totals, and grand totals.

```
GET /api/portfolio/stocks
```

**Example:**
```bash
curl -s http://localhost:8080/api/portfolio/stocks
```

**Sample Response (truncated):**
```json
{
  "data": [
    {
      "code": "ABNB",
      "name": "AIRBNB CL A ORD",
      "last": 122.92,
      "qty": 20,
      "mktval": 2458.4,
      "profit": -788.93,
      "ccy": "USD",
      "mkt": "NMS",
      ...
    }
  ],
  "entityId": "SG",
  "timestamp": "30/09/2025 19:44:50"
}
```

---

### 2. Get Unit Trust Portfolio

Returns the unit trust (mutual fund) holdings with NAV, market values, and P&L.

```
GET /api/portfolio/unit-trust
```

**Example:**
```bash
curl -s http://localhost:8080/api/portfolio/unit-trust
```

**Sample Response (truncated):**
```json
[
  {
    "investmentAccountNo": "612345789-1",
    "fundCode": "069036-318",
    "fundName": "SCHRODER ASIAN INCOME (SGD)",
    "availableUnits": 24788.39,
    "nav": 0.884,
    "marketValueFundCcy": 21912.94,
    "unRealPLInAssetCcy": 1412.94,
    ...
  }
]
```

---

## Quick Test

Run both endpoints with a single command:

```bash
# Stocks
curl -s http://localhost:8080/api/portfolio/stocks | python3 -m json.tool

# Unit Trust
curl -s http://localhost:8080/api/portfolio/unit-trust | python3 -m json.tool
```

---

## Running Tests

```bash
mvn test
```
