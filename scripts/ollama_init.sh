#!/usr/bin/env bash
# DEPRECATED: this script is intentionally disabled.
# The project no longer requires local shell scripts to preload Ollama models.
# Use docker-compose run --rm ollama-init to perform a one-shot model pull:
#   docker-compose up -d postgres
#   docker-compose run --rm ollama-init
#   docker-compose up -d ollama

echo "This script is deprecated. Use: docker-compose run --rm ollama-init"
exit 0
