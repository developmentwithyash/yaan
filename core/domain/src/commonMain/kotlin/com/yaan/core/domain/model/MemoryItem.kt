package com.yaan.core.domain.model

import com.yaan.core.common.datetime.nowInstant
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class MemoryItem(
    val id: String,
    val subject: String,
    val predicate: String,
    val confidenceScore: Double,
    val citationSourceId: String? = null,
    val createdAt: Instant = nowInstant(),
    val expiresAt: Instant? = null,
)
