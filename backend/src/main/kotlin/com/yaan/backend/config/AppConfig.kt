package com.yaan.backend.config

/**
 * YAAN Backend Configuration
 *
 * Implements parameters from ADR-0002 & ADR-0003:
 * - Sub-150ms interaction ceiling
 * - Dynamic provider routing
 * - Blind state relay storage limits
 */
data class AppConfig(
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080,
    val host: String = System.getenv("HOST") ?: "0.0.0.0",
    val environment: String = System.getenv("ENVIRONMENT") ?: "development",
    val latencyCeilingMs: Long = 150L,
    val circuitBreakerThreshold: Int = 3,
    val circuitBreakerResetMs: Long = 10_000L,
    val groqApiKey: String? = System.getenv("GROQ_API_KEY"),
    val fireworksApiKey: String? = System.getenv("FIREWORKS_API_KEY")
)
