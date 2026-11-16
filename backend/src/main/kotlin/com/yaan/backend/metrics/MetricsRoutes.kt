package com.yaan.backend.metrics

import com.yaan.backend.sync.CrdtRelayService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.metricsRoutes(relayService: CrdtRelayService) {
    get("/metrics") {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = totalMemory - freeMemory

        val activeUsers = relayService.getActiveUserCount()
        val totalEnvelopes = relayService.getTotalEnvelopeCount()
        val highestLamport = relayService.getHighestLamport()

        val sb = StringBuilder()
        sb.append("# HELP jvm_memory_used_bytes Used JVM heap memory\n")
        sb.append("# TYPE jvm_memory_used_bytes gauge\n")
        sb.append("jvm_memory_used_bytes $usedMemory\n\n")

        sb.append("# HELP jvm_memory_max_bytes Maximum JVM heap memory\n")
        sb.append("# TYPE jvm_memory_max_bytes gauge\n")
        sb.append("jvm_memory_max_bytes $maxMemory\n\n")

        sb.append("# HELP jvm_memory_free_bytes Free JVM heap memory\n")
        sb.append("# TYPE jvm_memory_free_bytes gauge\n")
        sb.append("jvm_memory_free_bytes $freeMemory\n\n")

        sb.append("# HELP yaan_crdt_users_active Active distinct users with CRDT sync states\n")
        sb.append("# TYPE yaan_crdt_users_active gauge\n")
        sb.append("yaan_crdt_users_active $activeUsers\n\n")

        sb.append("# HELP yaan_crdt_envelopes_total Total stored CRDT sync envelopes\n")
        sb.append("# TYPE yaan_crdt_envelopes_total gauge\n")
        sb.append("yaan_crdt_envelopes_total $totalEnvelopes\n\n")

        sb.append("# HELP yaan_crdt_highest_lamport Highest Lamport clock observed across users\n")
        sb.append("# TYPE yaan_crdt_highest_lamport counter\n")
        sb.append("yaan_crdt_highest_lamport $highestLamport\n")

        call.respondText(
            text = sb.toString(),
            contentType = ContentType.Text.Plain.withParameter("version", "0.0.4"),
            status = HttpStatusCode.OK
        )
    }
}
