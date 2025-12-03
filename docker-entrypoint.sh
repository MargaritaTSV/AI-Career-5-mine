#!/usr/bin/env bash
set -euo pipefail

MODEL="${OLLAMA_MODEL:-llama3}"

echo "[ollama] Starting background service for provisioning..."
ollama serve > /tmp/ollama-bootstrap.log 2>&1 &
OLLAMA_PID=$!

for i in {1..30}; do
  if ollama list >/dev/null 2>&1; then
    break
  fi
  echo "[ollama] Waiting for API to start (${i}/30)..."
  sleep 1
  if ! kill -0 ${OLLAMA_PID} >/dev/null 2>&1; then
    echo "[ollama] Service exited unexpectedly" >&2
    exit 1
  fi
  if [[ ${i} -eq 30 ]]; then
    echo "[ollama] Service did not start in time" >&2
    exit 1
  fi
done

echo "[ollama] Ensuring model '${MODEL}' is available..."
if ! ollama list | awk '{print $1}' | grep -Fxq "${MODEL}"; then
  ollama pull "${MODEL}"
fi

echo "[ollama] Stopping provisioning service..."
kill ${OLLAMA_PID}
wait ${OLLAMA_PID} || true

echo "[ollama] Starting Ollama in foreground"
exec ollama serve
