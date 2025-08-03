package com.example.agent

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Interface for streaming responses from an LLM. */
interface LlmExecutor {
    fun stream(prompt: String): Flow<String>
}

/**
 * Simple echo executor used when no external LLM is configured.
 */
class EchoLlmExecutor : LlmExecutor {
    override fun stream(prompt: String): Flow<String> = flow {
        for (word in prompt.split(" ")) {
            emit("${'$'}word ")
            delay(200)
        }
    }
}

/**
 * OpenAI streaming executor using the Chat Completions API.
 */
class OpenAiExecutor(private val apiKey: String, private val model: String = "gpt-3.5-turbo") : LlmExecutor {
    private val client = HttpClient {
        install(HttpTimeout) { requestTimeoutMillis = 60_000 }
    }

    override fun stream(prompt: String): Flow<String> = flow {
        val response = client.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer ${'$'}apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "model": "${'$'}model",
                  "stream": true,
                  "messages": [{"role":"user","content":"${'$'}{prompt.replace("\"", "\\\"")}"}]
                }
                """.trimIndent()
            )
        }
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.startsWith("data:")) {
                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") break
                val json = Json.parseToJsonElement(payload)
                val token = json.jsonObject["choices"]
                    ?.jsonArray?.get(0)?.jsonObject?.get("delta")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.content
                if (token != null) emit(token)
            }
        }
    }
}

/**
 * Ollama executor for local models.
 */
class OllamaExecutor(private val host: String) : LlmExecutor {
    private val client = HttpClient {
        install(HttpTimeout) { requestTimeoutMillis = 60_000 }
    }

    override fun stream(prompt: String): Flow<String> = flow {
        val response = client.post("${'$'}host/api/generate") {
            contentType(ContentType.Application.Json)
            setBody("""{"model":"llama2","prompt":"${'$'}{prompt.replace("\"", "\\\"")}","stream":true}""")
        }
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            val json = Json.parseToJsonElement(line)
            val token = json.jsonObject["response"]?.jsonPrimitive?.content
            if (token != null) emit(token)
            if (json.jsonObject["done"]?.jsonPrimitive?.booleanOrNull == true) break
        }
    }
}
