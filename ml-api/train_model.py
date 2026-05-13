from pathlib import Path

import joblib
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.neural_network import MLPClassifier
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler


DATASET_PATH = Path("data/training_dataset.csv")
MODEL_PATH = Path("model/commodity_risk_model.pkl")

FEATURE_COLUMNS = [
    "priceChangePercent",
    "precipitation",
    "rootZoneSoilWetness",
    "temperatureMax",
    "temperatureMin"
]

TARGET_COLUMN = "riskLevel"


def main():
    if not DATASET_PATH.exists():
        raise FileNotFoundError(f"Dataset not found: {DATASET_PATH}")

    data = pd.read_csv(DATASET_PATH)

    validate_dataset(data)

    x = data[FEATURE_COLUMNS]
    y = data[TARGET_COLUMN]

    x_train, x_test, y_train, y_test = train_test_split(
        x,
        y,
        test_size=0.2,
        random_state=42,
        stratify=y
    )

    model = Pipeline([
        ("scaler", StandardScaler()),
        ("classifier", MLPClassifier(
            hidden_layer_sizes=(16, 8),
            activation="relu",
            solver="adam",
            max_iter=1500,
            random_state=42
        ))
    ])

    model.fit(x_train, y_train)

    accuracy = model.score(x_test, y_test)

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, MODEL_PATH)

    print("Model trained successfully")
    print(f"Accuracy: {accuracy:.2f}")
    print(f"Model saved at: {MODEL_PATH}")


def validate_dataset(data: pd.DataFrame):
    required_columns = FEATURE_COLUMNS + [TARGET_COLUMN]

    missing_columns = [
        column for column in required_columns
        if column not in data.columns
    ]

    if missing_columns:
        raise ValueError(f"Missing required columns: {missing_columns}")

    if data.empty:
        raise ValueError("Dataset is empty")

    if data[TARGET_COLUMN].nunique() < 2:
        raise ValueError("Dataset must contain at least two different risk levels")


if __name__ == "__main__":
    main()