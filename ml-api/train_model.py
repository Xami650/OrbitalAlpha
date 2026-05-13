from pathlib import Path

import joblib
import pandas as pd
from sklearn.metrics import classification_report, confusion_matrix
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
    "temperatureMin",
]

TARGET_COLUMN = "riskLevel"


def main():
    if not DATASET_PATH.exists():
        raise FileNotFoundError(f"Dataset not found: {DATASET_PATH}")

    data = pd.read_csv(DATASET_PATH)
    _validate_dataset(data)

    x = data[FEATURE_COLUMNS]
    y = data[TARGET_COLUMN]

    x_train, x_test, y_train, y_test = train_test_split(
        x, y, test_size=0.2, random_state=42, stratify=y
    )

    model = Pipeline([
        ("scaler", StandardScaler()),
        ("classifier", MLPClassifier(
            hidden_layer_sizes=(16, 8),
            activation="relu",
            solver="adam",
            max_iter=1500,
            random_state=42,
        )),
    ])

    model.fit(x_train, y_train)

    y_pred = model.predict(x_test)
    accuracy = model.score(x_test, y_test)

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, MODEL_PATH)

    _print_results(y_test, y_pred, accuracy, MODEL_PATH)


def _print_results(y_test, y_pred, accuracy: float, model_path: Path):
    labels = ["LOW", "MEDIUM", "HIGH"]
    print("\n" + "=" * 50)
    print(f"Accuracy: {accuracy:.2f}")
    print("\nClassification report:")
    print(classification_report(y_test, y_pred, labels=labels, zero_division=0))
    print("Confusion matrix (rows=actual, cols=predicted):")
    matrix = confusion_matrix(y_test, y_pred, labels=labels)
    header = f"{'':>8}" + "".join(f"{label:>8}" for label in labels)
    print(header)
    for label, row in zip(labels, matrix):
        print(f"{label:>8}" + "".join(f"{val:>8}" for val in row))
    print("=" * 50)
    print(f"Model saved at: {model_path}\n")


def _validate_dataset(data: pd.DataFrame):
    required_columns = FEATURE_COLUMNS + [TARGET_COLUMN]
    missing = [col for col in required_columns if col not in data.columns]

    if missing:
        raise ValueError(f"Missing required columns: {missing}")

    if data.empty:
        raise ValueError("Dataset is empty")

    if data[TARGET_COLUMN].nunique() < 2:
        raise ValueError("Dataset must contain at least two different risk levels")


if __name__ == "__main__":
    main()
