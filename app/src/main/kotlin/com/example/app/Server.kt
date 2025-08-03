package com.example.app

import com.example.agent.*
import com.example.app.TtsService
import com.example.app.StubTtsService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respondTextWriter
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.serialization.jackson.*
import io.ktor.http.*
import kotlinx.coroutines.flow.collect
import java.nio.file.Paths
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

/** Entry point launching the Ktor server. */
fun main() {
    val executor: LlmExecutor = when {
        System.getenv("OPENAI_API_KEY") != null -> OpenAiExecutor(System.getenv("OPENAI_API_KEY"))
        System.getenv("OLLAMA_HOST") != null -> OllamaExecutor(System.getenv("OLLAMA_HOST"))
        else -> EchoLlmExecutor()
    }
    val agent = DemoAgent(executor, HttpJsonTool(), MemoryStore(Paths.get("memory/voice.txt")), Telemetry.tracer)
    embeddedServer(Netty, port = 8080) { configure(agent, StubTtsService()) }.start(wait = true)
}

fun Application.configure(agent: ChatAgent, tts: TtsService = StubTtsService()) {
    install(ContentNegotiation) { jackson() }
    install(CallLogging)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "error")
        }
    }

    val tracer = Telemetry.tracer

    routing {
        // Serve static index.html
        get("/") {
            call.respondFile(java.io.File("web/index.html"))
        }

        get("/api/v1/stream") {
            val prompt = call.request.queryParameters["prompt"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "prompt required")
            val voiceMode = call.request.queryParameters["voiceMode"] ?: "off"

            val span = tracer.spanBuilder("http.stream").startSpan()
            span.addEvent("server.receive")
            val start = System.nanoTime()
            call.response.cacheControl(CacheControl.NoCache(null))
            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                write("event: started\ndata: ${'$'}start\n\n")
                var count = 0
                val collected = StringBuilder()
                agent.stream(prompt).collect { token ->
                    if (count == 0) {
                        val ttft = (System.nanoTime() - start) / 1_000_000
                        span.addEvent("llm.first_token", Attributes.of(AttributeKey.longKey("ttft_ms"), ttft))
                    }
                    count++
                    collected.append(token)
                    write("event: token\ndata: ${'$'}token\n\n")
                    flush()
                }
                val duration = (System.nanoTime() - start) / 1_000_000
                span.addEvent(
                    "stream.complete",
                    Attributes.of(
                        AttributeKey.longKey("tokens"), count.toLong(),
                        AttributeKey.longKey("duration_ms"), duration
                    )
                )
                if (voiceMode == "server") {
                    val audio = tts.synthesize(collected.toString())
                    val encoded = StubTtsService.encodeBase64(audio)
                    write("event: audio\ndata: ${'$'}encoded\n\n")
                }
                write("event: done\ndata: {\"tokens\":${'$'}count,\"durationMs\":${'$'}duration}\n\n")
                flush()
            }
            span.end()
        }

        get("/health/otel") {
            val span = tracer.spanBuilder("otel.health").startSpan()
            span.end()
            call.respondText("OK")
        }
    }
}
