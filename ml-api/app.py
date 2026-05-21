from pathlib import Path

import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from risk_rules import level_from_score, build_reason

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

    risk_score = _score_from_probabilities(classes, probabilities)
    risk_level = level_from_score(risk_score)

    metrics_dict = {col: getattr(request, col) for col in FEATURE_COLUMNS}
    reason = build_reason(metrics_dict, risk_level)

    return PredictionResponse(
        commodity=request.commodity.upper(),
        riskLevel=risk_level,
        riskScore=risk_score,
        reason=reason,
    )


def _score_from_probabilities(classes: list, probabilities) -> float:
    score_by_level = {
        "LOW": 10.0,
        "LOW_MEDIUM": 30.0,
        "MEDIUM": 50.0,
        "MEDIUM_HIGH": 70.0,
        "HIGH": 90.0,
    }
    score = sum(score_by_level.get(str(c), 0.0) * p for c, p in zip(classes, probabilities))
    return round(score, 2)
