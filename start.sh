#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PIDS=()

cleanup() {
    echo ""
    echo "Stopping all services..."
    for pid in "${PIDS[@]}"; do
        kill "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null
    echo "All services stopped."
}
trap cleanup EXIT INT TERM

echo "=== 1/5 Starting ML API ==="
cd "$ROOT_DIR/ml-api"
if [ ! -d ".venv" ]; then
    echo "Creating Python venv..."
    python3 -m venv .venv
fi
source .venv/bin/activate
pip install -q -r requirements.txt
python train_model.py
uvicorn app:app --port 8000 &
PIDS+=($!)
cd "$ROOT_DIR"

echo "Waiting for ML API..."
until curl -s http://localhost:8000/health > /dev/null 2>&1; do
    sleep 1
done
echo "ML API is ready."

echo ""
echo "=== 2/5 Starting EventStoreBuilder ==="
mvn -q exec:java -pl eventstorebuilder &
PIDS+=($!)
sleep 3

echo ""
echo "=== 3/5 Starting BusinessUnit ==="
mvn -q exec:java -pl businessunit &
PIDS+=($!)
sleep 3
echo ""
echo "=== 4/5 Starting WeatherFeeder ==="
mvn -q exec:java -pl weatherfeeder -Dexec.args="weatherfeeder/config/producers.csv" &
PIDS+=($!)


echo ""
echo "=== 5/5 Starting MarketFeeder ==="
mvn -q exec:java -pl marketfeeder &
PIDS+=($!)

echo ""
echo "All services started. Press Ctrl+C to stop all."
wait
