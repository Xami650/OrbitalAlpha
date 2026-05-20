from fastapi.testclient import TestClient

from app import app

client = TestClient(app)

VALID_REQUEST = {
    "commodity": "WEAT",
    "priceChangePercent": 6.5,
    "precipitation": 0.2,
    "rootZoneSoilWetness": 0.28,
    "temperatureMax": 34.0,
    "temperatureMin": 18.0,
    "priceVolatility": 3.0,
    "priceTrend": 2.0,
    "precipitationDelta": -1.0,
    "soilWetnessDelta": -0.1,
    "temperatureMaxDelta": 2.0,
}

VALID_RISK_LEVELS = {"LOW", "LOW_MEDIUM", "MEDIUM", "MEDIUM_HIGH", "HIGH"}


def test_health_returns_200_with_status_and_model_loaded():
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "OK"
    assert "modelLoaded" in body


def test_predict_valid_returns_200_with_expected_fields():
    response = client.post("/predict", json=VALID_REQUEST)
    assert response.status_code == 200
    body = response.json()
    assert "commodity" in body
    assert "riskLevel" in body
    assert "riskScore" in body
    assert "reason" in body


def test_predict_valid_risk_level_is_valid():
    response = client.post("/predict", json=VALID_REQUEST)
    assert response.status_code == 200
    body = response.json()
    assert body["riskLevel"] in VALID_RISK_LEVELS


def test_predict_valid_risk_score_is_float_between_0_and_100():
    response = client.post("/predict", json=VALID_REQUEST)
    assert response.status_code == 200
    body = response.json()
    score = body["riskScore"]
    assert isinstance(score, (int, float))
    assert 0 <= score <= 100


def test_predict_missing_fields_returns_422():
    incomplete_request = {"commodity": "WEAT", "priceChangePercent": 6.5}
    response = client.post("/predict", json=incomplete_request)
    assert response.status_code == 422


LEVEL_THRESHOLDS = {
    "HIGH": (80, 100),
    "MEDIUM_HIGH": (60, 80),
    "MEDIUM": (40, 60),
    "LOW_MEDIUM": (20, 40),
    "LOW": (0, 20),
}


def test_predict_risk_level_is_consistent_with_score():
    response = client.post("/predict", json=VALID_REQUEST)
    assert response.status_code == 200
    body = response.json()
    level = body["riskLevel"]
    score = body["riskScore"]
    low, high = LEVEL_THRESHOLDS[level]
    assert low <= score <= 100 if level == "HIGH" else low <= score < high, (
        f"Score {score} is inconsistent with level {level} (expected [{low}, {high}))"
    )


def test_predict_commodity_matches_request_uppercased():
    lowercase_request = {**VALID_REQUEST, "commodity": "weat"}
    response = client.post("/predict", json=lowercase_request)
    assert response.status_code == 200
    body = response.json()
    assert body["commodity"] == "WEAT"
