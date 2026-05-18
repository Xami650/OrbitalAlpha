from pathlib import Path

import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

MODEL_PATH = Path("model/commodity_risk_model.pkl")

FEATURE_COLUMNS = [
    "priceChangePercent",
    "precipitation",
    "rootZoneSoilWetness",
    "temperatureMax",
    "temperatureMin",
    "priceVolatility",
    "priceTrend",
    "precipitationDelta",
    "soilWetnessDelta",
    "temperatureMaxDelta",
]

app = FastAPI(title="Commodity Risk ML API")

model = joblib.load(MODEL_PATH) if MODEL_PATH.exists() else None


class PredictionRequest(BaseModel):
    commodity: str
    priceChangePercent: float
    precipitation: float
    rootZoneSoilWetness: float
    temperatureMax: float
    temperatureMin: float
    priceVolatility: float
    priceTrend: float
    precipitationDelta: float
    soilWetnessDelta: float
    temperatureMaxDelta: float


class PredictionResponse(BaseModel):
    commodity: str
    riskLevel: str
    riskScore: float
    reason: str


@app.get("/health")
def health():
    return {"status": "OK", "modelLoaded": model is not None}


@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest):
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    features = [[getattr(request, col) for col in FEATURE_COLUMNS]]

    probabilities = model.predict_proba(features)[0]
    classes = list(model.classes_)

    risk_level = str(classes[probabilities.argmax()])
    risk_score = _score_from_probabilities(classes, probabilities)

    return PredictionResponse(
        commodity=request.commodity.upper(),
        riskLevel=risk_level,
        riskScore=risk_score,
        reason=_build_reason(request, risk_level),
    )


def _score_from_probabilities(classes: list, probabilities) -> float:
    score_by_level = {"LOW": 20.0, "MEDIUM": 55.0, "HIGH": 90.0}
    score = sum(score_by_level.get(str(c), 0.0) * p for c, p in zip(classes, probabilities))
    return round(score, 2)


def _build_reason(request: PredictionRequest, risk_level: str) -> str:
    reasons = []

    if request.priceChangePercent > 5:
        reasons.append("strong price increase")
    elif request.priceChangePercent > 2:
        reasons.append("moderate price increase")

    if request.priceVolatility > 4:
        reasons.append("high price volatility")

    if request.priceTrend > 3:
        reasons.append("sustained upward price trend")

    if request.precipitation < 1:
        reasons.append("low precipitation")

    if request.precipitationDelta < -2:
        reasons.append("precipitation below recent average")

    if 0 < request.rootZoneSoilWetness < 0.35:
        reasons.append("low root-zone soil wetness")

    if request.soilWetnessDelta < -0.15:
        reasons.append("soil wetness below recent average")

    if request.temperatureMax > 32:
        reasons.append("high maximum temperature")

    if request.temperatureMaxDelta > 5:
        reasons.append("temperature above recent average")

    if request.temperatureMin < 3:
        reasons.append("low minimum temperature")

    if not reasons:
        return f"{risk_level} risk: market and weather indicators remain stable."

    return f"{risk_level} risk due to " + ", ".join(reasons) + "."
