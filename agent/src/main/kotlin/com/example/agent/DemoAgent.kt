package com.example.agent

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

/**
 * Minimal agent demonstrating a node pipeline with optional tool call
 * and streaming of LLM tokens.
 */
class DemoAgent(
    private val executor: LlmExecutor,
    private val tool: HttpJsonTool,
    private val memory: MemoryStore,
    private val tracer: io.opentelemetry.api.trace.Tracer
) : ChatAgent {
    override fun stream(prompt: String): Flow<String> = flow {
        val span = tracer.spanBuilder("agent.run").startSpan()
        val start = System.nanoTime()
        try {
            // Node A: update prompt/history (and optional voice preference)
            var historyPrompt = prompt
            if (prompt.startsWith("voice:", ignoreCase = true)) {
                memory.writeVoicePreference(prompt.substringAfter(":").trim())
            }
            val voicePref = memory.readVoicePreference()
            voicePref?.let { span.setAttribute("memory.voice", it) }
            // Node B: optional tool call if keyword present
            if (prompt.contains("json", ignoreCase = true)) {
                val toolSpan = tracer.spanBuilder("tool.http").startSpan()
                try {
                    val data = tool.invoke("https://jsonplaceholder.typicode.com/todos/1", "")
                    toolSpan.setAttribute("tool.result.length", data.length.toLong())
                } finally {
                    toolSpan.end()
                }
            }
            // Node C: stream tokens to client
            var emitted = 0
            executor.stream(historyPrompt).collect { token ->
                if (emitted == 0) {
                    val ttft = (System.nanoTime() - start) / 1_000_000
                    span.addEvent("llm.first_token", Attributes.of(AttributeKey.longKey("ttft_ms"), ttft))
                }
                emitted++
                emit(token)
            }
            val duration = (System.nanoTime() - start) / 1_000_000
            span.addEvent(
                "stream.complete",
                Attributes.of(
                    AttributeKey.longKey("tokens"), emitted.toLong(),
                    AttributeKey.longKey("duration_ms"), duration
                )
            )
        } finally {
            span.end()
        }
    }
}

interface ChatAgent {
    fun stream(prompt: String): Flow<String>
}
