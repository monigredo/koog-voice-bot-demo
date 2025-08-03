package com.example.agent

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple file-based memory used to persist a single preference (voice).
 */
class MemoryStore(private val path: Path) {
    private val mutex = Mutex()

    suspend fun readVoicePreference(): String? = mutex.withLock {
        if (!Files.exists(path)) return null
        Files.readString(path).ifBlank { null }
    }

    suspend fun writeVoicePreference(pref: String) = mutex.withLock {
        Files.createDirectories(path.parent)
        Files.writeString(path, pref, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }
}
