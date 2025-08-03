package com.example.app

import com.example.agent.ChatAgent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue
import io.ktor.server.testing.testApplication
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readUTF8Line

class StreamingTest {
    @Test
    fun firstChunkArrives() = testApplication {
        val fakeAgent = object : ChatAgent {
            override fun stream(prompt: String): Flow<String> = flow {
                delay(100)
                emit("hello")
                delay(100)
                emit(" world")
            }
        }
        application { configure(fakeAgent) }

        val response = client.get("/api/v1/stream?prompt=hi")
        val channel = response.bodyAsChannel()
        val line = withTimeout(1000) { channel.readUTF8Line()!! }
        assertTrue(line.contains("started"))
    }
}
