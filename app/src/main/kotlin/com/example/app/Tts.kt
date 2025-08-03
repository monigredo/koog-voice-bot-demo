package com.example.app

import kotlin.math.PI
import kotlin.math.sin
import java.util.Base64

interface TtsService {
    fun synthesize(text: String): ByteArray
}

/**
 * Stub TTS that generates one second of 440Hz sine wave regardless of input.
 */
class StubTtsService : TtsService {
    override fun synthesize(text: String): ByteArray {
        val sampleRate = 8000
        val duration = 1
        val samples = ByteArray(sampleRate * duration) {
            val t = it / sampleRate.toDouble()
            (sin(2 * PI * 440 * t) * 127).toInt().toByte()
        }
        return samples
    }

    companion object {
        fun encodeBase64(data: ByteArray): String = Base64.getEncoder().encodeToString(data)
    }
}
