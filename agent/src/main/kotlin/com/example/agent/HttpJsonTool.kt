package com.example.agent

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import kotlinx.coroutines.delay

/**
 * Simple HTTP GET tool that retrieves JSON from a public endpoint with retry/backoff.
 */
class HttpJsonTool(
    private val client: HttpClient = HttpClient(CIO) { install(HttpTimeout) { requestTimeoutMillis = 5_000 } }
) {
    suspend fun invoke(url: String, query: String): String {
        var last: Throwable? = null
        repeat(3) { attempt ->
            try {
                val response = client.get(url) {
                    parameter("q", query)
                    timeout { requestTimeoutMillis = 5_000 }
                }
                if (response.status.isSuccess()) {
                    return response.bodyAsText()
                } else {
                    last = RuntimeException("HTTP ${'$'}{response.status.value}")
                }
            } catch (e: Throwable) {
                last = e
            }
            delay(500L * (attempt + 1))
        }
        throw last ?: IllegalStateException("Unknown error")
    }
}
