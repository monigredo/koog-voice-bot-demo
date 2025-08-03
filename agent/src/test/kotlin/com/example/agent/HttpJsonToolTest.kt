package com.example.agent

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

class HttpJsonToolTest {
    @Test
    fun happyPath() = runBlocking {
        val engine = MockEngine { _ ->
            respond("""{"message":"ok"}""", HttpStatusCode.OK)
        }
        val client = HttpClient(engine) {
            install(HttpTimeout) { requestTimeoutMillis = 5_000 }
        }
        val tool = HttpJsonTool(client)
        val result = tool.invoke("http://test", "q")
        assertEquals("{\"message\":\"ok\"}", result)
    }

    @Test
    fun timeout() = runBlocking {
        val engine = MockEngine { _ ->
            delay(10_000)
            respond("late", HttpStatusCode.OK)
        }
        val client = HttpClient(engine) {
            install(HttpTimeout) { requestTimeoutMillis = 100 }
        }
        val tool = HttpJsonTool(client)
        assertFailsWith<Exception> {
            tool.invoke("http://test", "q")
        }
    }
}
