package com.yaan.backend

import com.yaan.backend.briefing.BriefingWorkerService
import com.yaan.backend.briefing.briefingRoutes
import com.yaan.backend.config.AppConfig
import com.yaan.backend.inference.InferenceMeshService
import com.yaan.backend.inference.inferenceRoutes
import com.yaan.backend.sync.CrdtRelayService
import com.yaan.backend.sync.syncRoutes
import com.yaan.backend.vault.VaultBackupService
import com.yaan.backend.vault.vaultRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val config = AppConfig()
    embeddedServer(Netty, port = config.port, host = config.host) {
        module(config)
    }.start(wait = true)
}

fun Application.module(
    config: AppConfig = AppConfig(),
    relayService: CrdtRelayService = CrdtRelayService(),
    inferenceService: InferenceMeshService = InferenceMeshService(config),
    vaultService: VaultBackupService = VaultBackupService(),
    briefingService: BriefingWorkerService = BriefingWorkerService()
) {
    // 1. Serialization
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    // 2. CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }

@kotlinx.serialization.Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val latencyCeilingMs: Long,
    val invariants: List<String>
)

@kotlinx.serialization.Serializable
data class ErrorResponse(
    val status: String,
    val message: String
)

    // 3. Status Pages / Error Handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val safeMessage = if (config.environment == "production") {
                "An unexpected internal error occurred."
            } else {
                cause.message ?: "Internal Server Error"
            }
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    status = "ERROR",
                    message = safeMessage
                )
            )
        }
    }

    // 4. Routing
    routing {
        // Health & Diagnostics
        get("/healthz") {
            call.respond(
                HttpStatusCode.OK,
                HealthResponse(
                    status = "HEALTHY",
                    service = "YAAN Asymmetric Acceleration Hub",
                    version = "2.0.0-GOLD-MASTER",
                    latencyCeilingMs = config.latencyCeilingMs,
                    invariants = listOf(
                        "Deterministic State Decoupled from Probabilistic AI",
                        "Zero-Knowledge E2EE Blind Relay",
                        "Sub-150ms Interaction Ceiling",
                        "Pre-computed Async Nightly Intelligence"
                    )
                )
            )
        }

        // Domain Routes
        syncRoutes(relayService)
        inferenceRoutes(inferenceService)
        vaultRoutes(vaultService)
        briefingRoutes(briefingService)
    }
}
