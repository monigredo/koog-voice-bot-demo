# Koog Voice Bot Demo

Small demo showcasing:

1. **Real-time streaming responses** over SSE.
2. **Agentic workflow** with an optional HTTP JSON tool and local memory.
3. **Observability** via OpenTelemetry exporting to Jaeger with custom span events (TTFT, stream timings).
4. **CI/CD** pipeline using GitHub Actions and Jib container builds.

## Prerequisites
- JDK 21
- Docker
- Optional: `OPENAI_API_KEY` for OpenAI or `OLLAMA_HOST` for local models.

## Quickstart
```bash
# Start Jaeger
cd infra
docker compose up -d

# Choose your model provider
export OPENAI_API_KEY=sk-...
# or
# export OLLAMA_HOST=http://localhost:11434

# Run the server
cd ..
./gradlew :app:run
```
Visit [http://localhost:8080](http://localhost:8080) and enter a prompt. Tokens stream in real time and latency metrics update live.

```bash
curl -N "http://localhost:8080/api/v1/stream?prompt=hello"
```
Open Jaeger at [http://localhost:16686](http://localhost:16686) to inspect spans for agent run, nodes, LLM and tool calls.

## Environment Variables
- `OPENAI_API_KEY` – use OpenAI for responses.
- `OLLAMA_HOST` – URL for a local Ollama server.
- `VOICE_MODE` – `off` (default), `client`, or `server` for TTS.

## Troubleshooting
- No spans? Ensure Jaeger is running and `4317` is reachable.
- The demo always enables a logging span exporter for quick inspection.
- Increase sampling by adjusting OpenTelemetry configuration in `Telemetry.kt` if needed.
