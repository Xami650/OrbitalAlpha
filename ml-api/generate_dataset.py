"""
Generates training_dataset.csv from businessunit_batch.db.

Strategy:
  - Price changes (priceChangePercent) are computed from the full 10-year
    weekly price history stored in the batch datamart. All values are real.
  - Weather metrics are summarised into three representative scenarios per
    commodity (LOW, MID, HIGH adverse conditions) derived from the actual
    weather readings using the P10, P50 and P90 percentiles of each metric.
    This ensures the dataset covers the full range of conditions that the
    deployed system actually observes.
  - Each combination of price change × weather scenario produces one row.
  - Risk labels are assigned using the same domain rules as
    HeuristicRiskPredictor (bootstrap labeling, no human ground truth).

Note: once weatherfeeder's historical improvement is deployed (520-week
lookback), re-running this script will automatically produce a richer
dataset that incorporates real extreme weather events.

Usage:
    python generate_dataset.py
    python generate_dataset.py --db-path /path/to/businessunit_batch.db
    python generate_dataset.py --db-path ../businessunit_batch.db --output data/training_dataset.csv
"""

import argparse
import json
import sqlite3
from pathlib import Path

import pandas as pd
import numpy as np

OUTPUT_PATH = Path("data/training_dataset.csv")
DEFAULT_DB_PATH = Path("../businessunit_batch.db")

COMMODITY_TYPE_TO_SYMBOL = {
    "WHEAT": "WEAT",
    "CORN": "CORN",
    "SOYBEANS": "SOYB",
    "SOYBEAN": "SOYB",
    "SOY": "SOYB",
    "SOY_BEANS": "SOYB",
    "COFFEE": "JO",
    "NATURAL_GAS": "UNG",
    "NATURAL GAS": "UNG",
    "GAS": "UNG",
}

WEATHER_METRICS = ["precipitation", "rootZoneSoilWetness", "temperatureMax", "temperatureMin"]


def main():
    args = _parse_args()
    db_path = Path(args.db_path)
    output_path = Path(args.output)

    if not db_path.exists():
        raise FileNotFoundError(f"Batch datamart not found: {db_path}")

    print(f"Reading batch datamart: {db_path}")
    events = _load_events(db_path)
    print(f"Loaded {len(events)} historical events")

    price_changes = _extract_price_changes(events)
    weather_scenarios = _extract_weather_scenarios(events)

    if not price_changes:
        raise ValueError("No price data found. Run businessunit first to populate the batch datamart.")

    if not weather_scenarios:
        raise ValueError("No weather data found. Run businessunit first to populate the batch datamart.")

    rows = _build_dataset_rows(price_changes, weather_scenarios)
    df = pd.DataFrame(rows)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(output_path, index=False)

    print(f"Dataset generated: {len(df)} rows → {output_path}")
    print(df["riskLevel"].value_counts().to_string())


def _parse_args():
    parser = argparse.ArgumentParser(description="Generate training dataset from batch datamart")
    parser.add_argument("--db-path", default=str(DEFAULT_DB_PATH), help="Path to businessunit_batch.db")
    parser.add_argument("--output", default=str(OUTPUT_PATH), help="Output CSV path")
    return parser.parse_args()


def _load_events(db_path: Path) -> list[dict]:
    with sqlite3.connect(db_path) as conn:
        cursor = conn.execute("SELECT topic, raw_json FROM historical_events")
        return [{"topic": row[0], "raw_json": row[1]} for row in cursor.fetchall()]


def _extract_price_changes(events: list[dict]) -> list[float]:
    """
    Computes all weekly priceChangePercent values across all commodities.
    Deduplicates by (symbol, date) to avoid repeats from multiple feeder runs.
    """
    latest_by_key: dict[tuple, float] = {}

    for event in events:
        if event["topic"].lower() != "commodityprice":
            continue
        try:
            data = json.loads(event["raw_json"])
        except json.JSONDecodeError:
            continue

        symbol = data.get("symbol", "").upper()
        price_ts = data.get("priceTimestamp", "")
        close = data.get("close")

        if not symbol or not price_ts or close is None:
            continue

        latest_by_key[(symbol, price_ts[:10])] = float(close)

    by_symbol: dict[str, list[tuple]] = {}
    for (symbol, date), close in latest_by_key.items():
        by_symbol.setdefault(symbol, []).append((date, close))

    changes = []
    for entries in by_symbol.values():
        entries.sort(key=lambda e: e[0])
        for i in range(1, len(entries)):
            prev = entries[i - 1][1]
            curr = entries[i][1]
            if prev != 0.0:
                changes.append(round((curr - prev) / prev * 100.0, 4))

    return changes


def _extract_weather_scenarios(events: list[dict]) -> list[dict]:
    """
    Returns three representative weather scenarios derived from all available
    weather readings: P10 (adverse), P50 (moderate), P90 (benign) for each
    metric, combined to form three distinct conditions.
    """
    readings = []

    seen: set[tuple] = set()
    for event in events:
        if event["topic"].lower() != "weather":
            continue
        try:
            data = json.loads(event["raw_json"])
        except json.JSONDecodeError:
            continue

        commodity_type = data.get("commodityType", "").upper()
        symbol = COMMODITY_TYPE_TO_SYMBOL.get(commodity_type)
        raw_date = data.get("date", "")

        if not symbol or len(raw_date) != 8:
            continue

        key = (symbol, raw_date)
        if key in seen:
            continue
        seen.add(key)

        readings.append({
            "precipitation": float(data.get("precipitation", 0.0)),
            "rootZoneSoilWetness": float(data.get("rootZoneSoilWetness", 0.0)),
            "temperatureMax": float(data.get("temperatureMax", 0.0)),
            "temperatureMin": float(data.get("temperatureMin", 0.0)),
        })

    if not readings:
        return []

    df = pd.DataFrame(readings)
    p50 = df[WEATHER_METRICS].quantile(0.50)

    actual_min = df[WEATHER_METRICS].min()
    actual_max = df[WEATHER_METRICS].max()

    # Adverse: actual extreme values that stress-test the heuristic thresholds.
    # Uses real observed minimums/maximums, not synthetic values.
    adverse = {
        "precipitation": round(float(actual_min["precipitation"]), 4),
        "rootZoneSoilWetness": round(float(actual_min["rootZoneSoilWetness"]), 4),
        "temperatureMax": round(float(actual_max["temperatureMax"]), 4),
        "temperatureMin": round(float(actual_min["temperatureMin"]), 4),
    }
    # Moderate: median values
    moderate = {metric: round(float(p50[metric]), 4) for metric in WEATHER_METRICS}
    # Benign: high precipitation, high soil wetness, low tempMax, high tempMin
    benign = {
        "precipitation": round(float(actual_max["precipitation"]), 4),
        "rootZoneSoilWetness": round(float(actual_max["rootZoneSoilWetness"]), 4),
        "temperatureMax": round(float(actual_min["temperatureMax"]), 4),
        "temperatureMin": round(float(actual_max["temperatureMin"]), 4),
    }

    return [adverse, moderate, benign]


def _build_dataset_rows(price_changes: list[float], weather_scenarios: list[dict]) -> list[dict]:
    rows = []
    for change in price_changes:
        for weather in weather_scenarios:
            row = {"priceChangePercent": change, **weather}
            row["riskLevel"] = _assign_risk_level(row)
            rows.append(row)
    return rows


def _assign_risk_level(row: dict) -> str:
    score = 0.0

    if row["priceChangePercent"] > 5:
        score += 30
    elif row["priceChangePercent"] > 2:
        score += 15

    if row["precipitation"] < 1:
        score += 15

    if 0 < row["rootZoneSoilWetness"] < 0.35:
        score += 20

    if row["temperatureMax"] > 32:
        score += 15

    if row["temperatureMin"] < 3:
        score += 15

    if score >= 70:
        return "HIGH"
    if score >= 40:
        return "MEDIUM"
    return "LOW"


if __name__ == "__main__":
    main()
