from pathlib import Path

import joblib
from fastapi import FastAPI
from pydantic import BaseModel


MODEL_PATH = Path("model/commodity_risk_model.pkl")

FEATURE_COLUMNS = [
    "priceChangePercent",
    "precipitation",
    "rootZoneSoilWetness",
    "temperatureMax",
    "temperatureMin"
]

app = FastAPI(title="Commodity Risk ML API")


class PredictionRequest(BaseModel):
    commodity: str
    priceChangePercent: float
    precipitation: float
    rootZoneSoilWetness: float
    temperatureMax: float
    temperatureMin: float


class PredictionResponse(BaseModel):
    commodity: str
    riskLevel: str
    riskScore: float
    reason: str


def load_model():
    if not MODEL_PATH.exists():
        return None

    return joblib.load(MODEL_PATH)


@app.get("/health")
def health():
    return {
        "status": "OK",
        "modelLoaded": MODEL_PATH.exists()
    }


@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest):
    model = load_model()

    if model is None:
        risk_score = calculate_fallback_score(request)
        risk_level = score_to_risk_level(risk_score)

        return PredictionResponse(
            commodity=request.commodity.upper(),
            riskLevel=risk_level,
            riskScore=risk_score,
            reason=build_reason(request, risk_level)
        )

    features = [[
        request.priceChangePercent,
        request.precipitation,
        request.rootZoneSoilWetness,
        request.temperatureMax,
        request.temperatureMin
    ]]

    probabilities = model.predict_proba(features)[0]
    classes = list(model.classes_)

    predicted_risk_level = model.predict(features)[0]

    risk_score = calculate_score_from_probabilities(classes, probabilities)

    return PredictionResponse(
        commodity=request.commodity.upper(),
        riskLevel=str(predicted_risk_level),
        riskScore=risk_score,
        reason=build_reason(request, str(predicted_risk_level))
    )


def calculate_score_from_probabilities(classes, probabilities) -> float:
    score_by_level = {
        "LOW": 20.0,
        "MEDIUM": 55.0,
        "HIGH": 90.0
    }

    score = 0.0

    for risk_class, probability in zip(classes, probabilities):
        score += score_by_level.get(str(risk_class), 0.0) * probability

    return round(score, 2)


def calculate_fallback_score(request: PredictionRequest) -> float:
    score = 0.0

    if request.priceChangePercent > 5:
        score += 30
    elif request.priceChangePercent > 2:
        score += 15

    if request.precipitation < 1:
        score += 15

    if 0 < request.rootZoneSoilWetness < 0.35:
        score += 20

    if request.temperatureMax > 32:
        score += 15

    if request.temperatureMin < 3:
        score += 15

    return round(min(score, 100.0), 2)


def score_to_risk_level(score: float) -> str:
    if score >= 70:
        return "HIGH"

    if score >= 40:
        return "MEDIUM"

    return "LOW"


def build_reason(request: PredictionRequest, risk_level: str) -> str:
    reasons = []

    if request.priceChangePercent > 5:
        reasons.append("strong price increase")
    elif request.priceChangePercent > 2:
        reasons.append("moderate price increase")

    if request.precipitation < 1:
        reasons.append("low precipitation")

    if 0 < request.rootZoneSoilWetness < 0.35:
        reasons.append("low root-zone soil wetness")

    if request.temperatureMax > 32:
        reasons.append("high maximum temperature")

    if request.temperatureMin < 3:
        reasons.append("low minimum temperature")

    if not reasons:
        return f"{risk_level} risk because market and weather indicators remain stable."

    return f"{risk_level} risk due to " + ", ".join(reasons) + "."