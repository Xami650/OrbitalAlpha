# 🛰️ Quant Crop Oracle: NASA-Driven Commodity Arbitrage

An automated information arbitrage system. It crosses NASA's satellite agro-climatic data with financial markets to detect severe weather anomalies (like droughts or floods) in key agricultural regions *before* commodity prices reflect the crop impact.

## 🎯 The Edge (Problem & Solution)

In commodity markets (coffee, wheat, cocoa, etc.), prices are critically dependent on seasonality and weather conditions during the blooming or planting months. By the time a drought hits mainstream news, institutional investors have already priced it in. 

**This script automates your edge:**
1. Reads soil moisture and precipitation data directly from NASA satellites for specific geographic coordinates.
2. Compares current levels with the 10-year historical average for that exact location and time of year.
3. If a critical anomaly is detected (e.g., -30% moisture during blooming season), it checks the underlying financial asset's current price.
4. Sends an early warning via Telegram if the market hasn't reacted yet (potential entry opportunity).



## ⚙️ Architecture & APIs Used

* **NASA POWER API:** For both historical baseline and real-time agrometeorological data extraction (specifically targeting parameters like `PRECTOTCORR` for precipitation and `GWETROOT` for root zone soil wetness).
* **Alpha Vantage / yfinance API:** To monitor the current price of futures contracts (e.g., Arabica Coffee `KC=F`) or related ETFs.
* **Telegram Bot API:** For the push notification and alert system.
* **Core Stack:** Python 3, `requests` (for HTTP calls), `pandas` (for time-series data analysis).

## 🚀 Workflow

1.  **Data Ingestion:** Set up the exact coordinates (Lat/Lon) of critical producing zones (e.g., Minas Gerais, Brazil for coffee).
2.  **Baseline Calculation:** The system calculates the historical moving average for soil moisture during the current week based on the last decade.
3.  **Anomaly Detection:** Evaluates standard deviations against the climatic norm.
4.  **Financial Cross-Check:** Verifies the recent volatility of the associated ticker in the stock market.
5.  **Execution:** Triggers an arbitrage alert if the disparity criteria are met.

## ⚠️ Disclaimer

> This repository and its code are strictly for educational and computer research purposes. **It does not constitute financial advice**. Commodity markets are highly volatile and influenced by multiple macroeconomic factors beyond local weather. Trade at your own risk.
