import json
from pathlib import Path

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

CONTRACT_PATH = Path(__file__).parent.parent / "docs" / "contracts" / "predict-contract.json"


# --- Health endpoint ---


def test_health_returns_200_with_status_and_model_loaded():
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "OK"
    assert "modelLoaded" in body


def test_health_model_loaded_is_boolean():
    response = client.get("/health")
    body = response.json()
    assert isinstance(body["modelLoaded"], bool)


# --- Predict endpoint: valid requests ---


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


def test_predict_risk_level_is_consistent_with_score():
    response = client.post("/predict", json=VALID_REQUEST)
    assert response.status_code == 200
    body = response.json()
    level = body["riskLevel"]
    score = body["riskScore"]
    LEVEL_THRESHOLDS = {
        "HIGH": (80, 100),
        "MEDIUM_HIGH": (60, 80),
        "MEDIUM": (40, 60),
        "LOW_MEDIUM": (20, 40),
        "LOW": (0, 20),
    }
    low, high = LEVEL_THRESHOLDS[level]
    if level == "HIGH":
        assert low <= score <= 100
    else:
        assert low <= score < high, (
            f"Score {score} is inconsistent with level {level} (expected [{low}, {high}))"
        )


def test_predict_commodity_matches_request_uppercased():
    lowercase_request = {**VALID_REQUEST, "commodity": "weat"}
    response = client.post("/predict", json=lowercase_request)
    assert response.status_code == 200
    body = response.json()
    assert body["commodity"] == "WEAT"


def test_predict_reason_is_nonempty_string():
    response = client.post("/predict", json=VALID_REQUEST)
    body = response.json()
    assert isinstance(body["reason"], str)
    assert len(body["reason"]) > 0


# --- Predict endpoint: invalid requests ---


def test_predict_missing_fields_returns_422():
    incomplete_request = {"commodity": "WEAT", "priceChangePercent": 6.5}
    response = client.post("/predict", json=incomplete_request)
    assert response.status_code == 422


def test_predict_empty_body_returns_422():
    response = client.post("/predict", json={})
    assert response.status_code == 422


def test_predict_wrong_type_returns_422():
    bad_request = {**VALID_REQUEST, "priceChangePercent": "not_a_number"}
    response = client.post("/predict", json=bad_request)
    assert response.status_code == 422


def test_predict_no_body_returns_422():
    response = client.post("/predict")
    assert response.status_code == 422


# --- Edge cases: extreme values ---


def test_predict_zero_values():
    zero_request = {
        "commodity": "CORN",
        "priceChangePercent": 0.0,
        "precipitation": 0.0,
        "rootZoneSoilWetness": 0.0,
        "temperatureMax": 0.0,
        "temperatureMin": 0.0,
        "priceVolatility": 0.0,
        "priceTrend": 0.0,
        "precipitationDelta": 0.0,
        "soilWetnessDelta": 0.0,
        "temperatureMaxDelta": 0.0,
    }
    response = client.post("/predict", json=zero_request)
    assert response.status_code == 200
    body = response.json()
    assert body["riskLevel"] in VALID_RISK_LEVELS
    assert 0 <= body["riskScore"] <= 100


def test_predict_negative_values():
    negative_request = {
        "commodity": "SOYB",
        "priceChangePercent": -10.0,
        "precipitation": 50.0,
        "rootZoneSoilWetness": 0.9,
        "temperatureMax": 15.0,
        "temperatureMin": 10.0,
        "priceVolatility": 0.5,
        "priceTrend": -5.0,
        "precipitationDelta": 10.0,
        "soilWetnessDelta": 0.5,
        "temperatureMaxDelta": -3.0,
    }
    response = client.post("/predict", json=negative_request)
    assert response.status_code == 200
    body = response.json()
    assert body["riskLevel"] in VALID_RISK_LEVELS


def test_predict_large_values():
    large_request = {
        "commodity": "UNG",
        "priceChangePercent": 999.0,
        "precipitation": 500.0,
        "rootZoneSoilWetness": 1.0,
        "temperatureMax": 60.0,
        "temperatureMin": -40.0,
        "priceVolatility": 100.0,
        "priceTrend": 100.0,
        "precipitationDelta": -100.0,
        "soilWetnessDelta": -1.0,
        "temperatureMaxDelta": 50.0,
    }
    response = client.post("/predict", json=large_request)
    assert response.status_code == 200
    body = response.json()
    assert 0 <= body["riskScore"] <= 100


# --- Contract tests ---


def test_contract_request_fields_match_api():
    if not CONTRACT_PATH.exists():
        return
    contract = json.loads(CONTRACT_PATH.read_text())
    required_fields = set(contract["request"]["requiredFields"].keys())

    response = client.post("/predict", json=VALID_REQUEST)
    assert response.status_code == 200

    assert required_fields.issubset(set(VALID_REQUEST.keys()))


def test_contract_response_fields_match_api():
    if not CONTRACT_PATH.exists():
        return
    contract = json.loads(CONTRACT_PATH.read_text())
    required_fields = set(contract["response"]["requiredFields"].keys())

    response = client.post("/predict", json=VALID_REQUEST)
    assert response.status_code == 200
    body = response.json()

    assert required_fields == set(body.keys())


def test_contract_risk_levels_match_api():
    if not CONTRACT_PATH.exists():
        return
    contract = json.loads(CONTRACT_PATH.read_text())
    contract_levels = set(contract["response"]["validRiskLevels"])

    assert contract_levels == VALID_RISK_LEVELS
