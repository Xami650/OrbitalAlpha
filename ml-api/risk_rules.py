import json
from pathlib import Path

RULES_FILE = Path(__file__).parent.parent / "risk-rules.json"

def _load_rules():
    if not RULES_FILE.exists():
        # Default fallback if file is missing (original rules)
        return {
            "thresholds": {
                "priceChangeStrong": 5.0, "priceChangeModerate": 2.0, "lowPrecipitation": 1.0,
                "lowSoilWetness": 0.35, "highTemperatureMax": 32.0, "lowTemperatureMin": 3.0,
                "highPriceVolatility": 4.0, "highPriceTrend": 3.0, "lowPrecipitationDelta": -2.0,
                "lowSoilWetnessDelta": -0.15, "highTemperatureMaxDelta": 5.0
            },
            "weights": {
                "priceChangeStrong": 30.0, "priceChangeModerate": 15.0, "lowPrecipitation": 15.0,
                "lowSoilWetness": 20.0, "highTemperatureMax": 15.0, "lowTemperatureMin": 15.0,
                "highPriceVolatility": 10.0, "highPriceTrend": 10.0, "lowPrecipitationDelta": 5.0,
                "lowSoilWetnessDelta": 5.0, "highTemperatureMaxDelta": 5.0
            },
            "levels": [
                { "minScore": 80.0, "label": "HIGH" },
                { "minScore": 60.0, "label": "MEDIUM_HIGH" },
                { "minScore": 40.0, "label": "MEDIUM" },
                { "minScore": 20.0, "label": "LOW_MEDIUM" },
                { "minScore": 0.0, "label": "LOW" }
            ]
        }
    with open(RULES_FILE, "r") as f:
        return json.load(f)

RULES = _load_rules()

def calculate_risk_score(metrics: dict) -> float:
    score = 0.0
    t = RULES["thresholds"]
    w = RULES["weights"]

    pcp = metrics.get("priceChangePercent", 0)
    if pcp > t["priceChangeStrong"]:
        score += w["priceChangeStrong"]
    elif pcp > t["priceChangeModerate"]:
        score += w["priceChangeModerate"]

    if metrics.get("precipitation", 0) < t["lowPrecipitation"]:
        score += w["lowPrecipitation"]

    rzsw = metrics.get("rootZoneSoilWetness", 0)
    if 0 < rzsw < t["lowSoilWetness"]:
        score += w["lowSoilWetness"]

    if metrics.get("temperatureMax", 0) > t["highTemperatureMax"]:
        score += w["highTemperatureMax"]

    if metrics.get("temperatureMin", 0) < t["lowTemperatureMin"]:
        score += w["lowTemperatureMin"]

    if metrics.get("priceVolatility", 0) > t["highPriceVolatility"]:
        score += w["highPriceVolatility"]

    if metrics.get("priceTrend", 0) > t["highPriceTrend"]:
        score += w["highPriceTrend"]

    if metrics.get("precipitationDelta", 0) < t["lowPrecipitationDelta"]:
        score += w["lowPrecipitationDelta"]

    if metrics.get("soilWetnessDelta", 0) < t["lowSoilWetnessDelta"]:
        score += w["lowSoilWetnessDelta"]

    if metrics.get("temperatureMaxDelta", 0) > t["highTemperatureMaxDelta"]:
        score += w["highTemperatureMaxDelta"]

    return min(score, 100.0)

def level_from_score(score: float) -> str:
    for level in RULES["levels"]:
        if score >= level["minScore"]:
            return level["label"]
    return "LOW"

def build_reason(metrics: dict, risk_level: str) -> str:
    reasons = []
    t = RULES["thresholds"]

    pcp = metrics.get("priceChangePercent", 0)
    if pcp > t["priceChangeStrong"]:
        reasons.append("strong price increase")
    elif pcp > t["priceChangeModerate"]:
        reasons.append("moderate price increase")

    if metrics.get("priceVolatility", 0) > t["highPriceVolatility"]:
        reasons.append("high price volatility")

    if metrics.get("priceTrend", 0) > t["highPriceTrend"]:
        reasons.append("sustained upward price trend")

    if metrics.get("precipitation", 0) < t["lowPrecipitation"]:
        reasons.append("low precipitation")

    if metrics.get("precipitationDelta", 0) < t["lowPrecipitationDelta"]:
        reasons.append("precipitation below recent average")

    rzsw = metrics.get("rootZoneSoilWetness", 0)
    if 0 < rzsw < t["lowSoilWetness"]:
        reasons.append("low root-zone soil wetness")

    if metrics.get("soilWetnessDelta", 0) < t["lowSoilWetnessDelta"]:
        reasons.append("soil wetness below recent average")

    if metrics.get("temperatureMax", 0) > t["highTemperatureMax"]:
        reasons.append("high maximum temperature")

    if metrics.get("temperatureMaxDelta", 0) > t["highTemperatureMaxDelta"]:
        reasons.append("temperature above recent average")

    if metrics.get("temperatureMin", 0) < t["lowTemperatureMin"]:
        reasons.append("low minimum temperature")

    if not reasons:
        return f"{risk_level} risk: market and weather indicators remain stable."

    return f"{risk_level} risk due to " + ", ".join(reasons) + "."
