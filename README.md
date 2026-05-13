# OrbitalAlpha — Sprint 3

Event-driven commodity risk analysis system implementing a **Lambda Architecture** inside the `businessunit` module.

---

## Architecture overview

```
WeatherFeeder ──┐
                ├──► ActiveMQ Topics ──► EventStoreBuilder ──► File Event Store
MarketFeeder ───┘                │
                                 ▼
                           BusinessUnit
                      ┌─────────────────────────────┐
                      │  Batch Layer                │
                      │    ↓                        │
                      │  businessunit_batch.db      │
                      │    ↓                        │
                      │  Serving Layer              │
                      │    ↓                        │
                      │  MlApiRiskPredictor ──► ml-api (Python)
                      │    ↓ (fallback: HeuristicRiskPredictor)
                      │  businessunit_serving.db    │
                      │    ↓                        │
                      │  Javalin REST API + Frontend│
                      └─────────────────────────────┘
                                 ↑
                           ActiveMQ (Speed Layer)
```

### Two datamarts

| Datamart | File | Purpose |
|---|---|---|
| Batch | `businessunit_batch.db` | Historical events rebuilt from the file event store |
| Serving | `businessunit_serving.db` | Latest risk snapshots, query-optimised for the API |

### ML-first prediction with heuristic fallback

`businessunit` calls `ml-api` by HTTP for each commodity risk prediction.
If `ml-api` is unavailable, `FallbackRiskPredictor` automatically switches to `HeuristicRiskPredictor`.
The frontend and `GET /api/status` indicate when fallback is active.

---

## Prerequisites

- Java 25
- Maven 3.x
- Python 3.11+
- Apache ActiveMQ Classic (or Docker)

---

## Local execution order

### 1. Start ActiveMQ

```bash
activemq console
```

Or with Docker:

```bash
docker run -p 61616:61616 -p 8161:8161 apache/activemq-classic:latest
```

### 2. Configure each module

Copy the example properties and fill in your values:

```bash
cp marketfeeder/src/main/resources/marketfeeder.example.properties \
   marketfeeder/src/main/resources/marketfeeder.properties

cp eventstorebuilder/src/main/resources/eventstorebuilder.example.properties \
   eventstorebuilder/src/main/resources/eventstorebuilder.example.properties
```

> `businessunit/src/main/resources/businessunit.properties` is ready to use with default local values.
>
> `weatherfeeder` currently has no properties file (broker URL is hardcoded). A future improvement will add a configurable weeks parameter.

### 3. Build all modules

```bash
mvn clean package
```

### 4. Start the Event Store Builder

```bash
mvn exec:java -pl eventstorebuilder
```

### 5. Set up and start ml-api

```bash
cd ml-api
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Generate training dataset from batch datamart (run after businessunit has populated batch DB)
python generate_dataset.py --db-path ../businessunit_batch.db

# Train the model
python train_model.py

# Start the API
uvicorn app:app --reload --port 8000
```

Verify:

```bash
curl http://localhost:8000/health
```

> **Note:** The model file (`commodity_risk_model.pkl`) is not committed to the repository.
> Always run `python train_model.py` to generate it before starting `ml-api`.

### 6. Start BusinessUnit

Set the working directory to the project root when running from IntelliJ:

```
/path/to/OrbitalAlpha
```

Then:

```bash
mvn exec:java -pl businessunit
```

### 7. Start the feeders

```bash
mvn exec:java -pl marketfeeder
mvn exec:java -pl weatherfeeder
```

---

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/health` | Health check |
| `GET` | `/api/status` | ML API availability (`{"mlAvailable": true/false}`) |
| `GET` | `/api/risks` | All commodity risk snapshots |
| `GET` | `/api/risks/{commodity}` | Risk snapshot for a single commodity (e.g. `WEAT`) |

Frontend: [http://localhost:7070](http://localhost:7070)

ml-api: [http://localhost:8000](http://localhost:8000)

---

## ML training dataset

The `generate_dataset.py` script extracts features from `businessunit_batch.db` and produces `ml-api/data/training_dataset.csv`:

```
eventstore → businessunit_batch.db → generate_dataset.py → training_dataset.csv → train_model.py → commodity_risk_model.pkl
```

Risk labels are assigned using the same domain rules as `HeuristicRiskPredictor` (bootstrap labeling, since no human-annotated ground truth is available). Three weather scenarios are derived from actual observations (benign, moderate, and adverse — using real measured extremes from the batch data), crossed with the full 10-year weekly price history.

> **Note on dataset quality:** `weatherfeeder` currently fetches only 30 days of weather history (hardcoded `DAYS_TO_FETCH = 30`). A future improvement by another team member will make this configurable, with an initial load of 520 weeks. Once deployed, re-running `generate_dataset.py` will automatically produce a richer dataset incorporating real historical extreme weather events.

---

## Running tests

```bash
# All modules
mvn clean test

# businessunit only
mvn test -pl businessunit
```

---

## Git hygiene

Files excluded from the repository (see `.gitignore`):

- `*.db` — generated SQLite datamarts
- `*.pkl` — generated ML model files (run `python train_model.py` to regenerate)
- `.venv/` — Python virtual environment
- `__pycache__/`
- `target/` — Maven build output
- `*.properties` — real config files (may contain API keys); only `businessunit.properties` is tracked

If a generated file was accidentally tracked:

```bash
git rm --cached <file>
```

---

## Module summary

| Module | Language | Role |
|---|---|---|
| `weatherfeeder` | Java | Polls NASA POWER and publishes weather events to ActiveMQ |
| `marketfeeder` | Java | Polls AlphaVantage and publishes commodity price events to ActiveMQ |
| `eventstorebuilder` | Java | Subscribes to ActiveMQ and appends events to the file event store |
| `businessunit` | Java | Lambda Architecture: batch, speed, serving layers; REST API; frontend |
| `ml-api` | Python | FastAPI neural network service for commodity risk prediction |