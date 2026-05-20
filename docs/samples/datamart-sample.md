# Datamart samples

## Batch datamart (`businessunit_batch.db`)

### Schema

```sql
CREATE TABLE historical_events (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    topic         TEXT NOT NULL,
    source_system TEXT NOT NULL,
    file_date     TEXT NOT NULL,
    raw_json      TEXT NOT NULL
);
```

### Sample rows

| id | topic | source_system | file_date | raw_json (truncated) |
|---|---|---|---|---|
| 1 | CommodityPrice | AlphaVantage | 20260512 | `{"ts":"2026-05-12T14:22:01...","symbol":"WEAT",...}` |
| 2 | CommodityPrice | AlphaVantage | 20260512 | `{"ts":"2026-05-12T14:22:01...","symbol":"CORN",...}` |
| 3 | Weather | weatherfeeder | 20260512 | `{"ts":"2026-05-12T14:30:16...","producerId":"WHEAT_1",...}` |

---

## Serving datamart (`businessunit_serving.db`)

### Schema

```sql
CREATE TABLE commodity_risk_snapshots (
    commodity  TEXT PRIMARY KEY,
    risk_level TEXT NOT NULL,
    risk_score REAL NOT NULL,
    reason     TEXT NOT NULL
);
```

### Sample rows

| commodity | risk_level | risk_score | reason |
|---|---|---|---|
| WEAT | MEDIUM_HIGH | 70.0 | MEDIUM_HIGH risk due to moderate price increase, low root-zone soil wetness. |
| CORN | LOW | 10.0 | LOW risk: market and weather indicators remain stable. |
| SOYB | HIGH | 90.0 | HIGH risk due to strong price increase, low precipitation, low root-zone soil wetness, high maximum temperature. |
| JO | MEDIUM_LOW | 30.0 | MEDIUM_LOW risk due to moderate price increase. |
| UNG | LOW | 10.0 | LOW risk: market and weather indicators remain stable. |
