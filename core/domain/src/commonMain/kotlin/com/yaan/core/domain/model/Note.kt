package com.yaan.core.domain.model

import com.yaan.core.common.datetime.nowInstant
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class VaultTag(
    val name: String,
    val isEncrypted: Boolean = true,
)

@Serializable
data class AudioMemoMetadata(
    val audioUri: String,
    val durationMs: Long,
    val waveformSamples: List<Float> = emptyList(),
)

@Serializable
data class Note(
    val id: String,
    val title: String,
    val contentCiphertext: String,
    val tags: List<VaultTag> = emptyList(),
    val audioMemo: AudioMemoMetadata? = null,
    val isPinned: Boolean = false,
    val createdAt: Instant = nowInstant(),
    val updatedAt: Instant = nowInstant(),
    val ttlDays: Int? = null,
)
