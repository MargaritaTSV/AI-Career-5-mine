FROM ollama/ollama:latest

# Expose the Ollama API
EXPOSE 11434

# Make a helper entrypoint that pre-pulls the requested model and then runs the server
COPY docker-entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
