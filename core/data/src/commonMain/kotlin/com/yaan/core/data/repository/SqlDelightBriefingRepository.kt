package com.yaan.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.db.BriefingEntity
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.domain.model.Briefing
import com.yaan.core.domain.model.ScheduleConflict
import com.yaan.core.domain.repository.BriefingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class SqlDelightBriefingRepository(
    private val database: YaanDatabase,
    private val dispatchers: CoroutineDispatchers,
) : BriefingRepository {
    private val queries = database.yaanDatabaseQueries

    override fun observeLatestBriefing(): Flow<Briefing?> {
        return queries.selectLatestBriefing()
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
            .map { it?.toDomain() }
    }

    override suspend fun getBriefingForDate(date: LocalDate): YaanResult<Briefing?> = withContext(dispatchers.io) {
        try {
            val entity = queries.selectLatestBriefing().executeAsOneOrNull()
            YaanResult.Success(entity?.toDomain())
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to fetch briefing: ${e.message}", e))
        }
    }

    override suspend fun saveBriefing(briefing: Briefing): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            val conflict = briefing.conflicts.firstOrNull()
            queries.insertBriefing(
                id = briefing.id,
                headline = "Daily Briefing",
                summary = briefing.summaryMarkdown,
                date = briefing.targetDate.toString(),
                is_listened = false,
                conflict_title = conflict?.description,
                conflict_slot = conflict?.targetTime?.toString(),
                conflict_resolution = conflict?.proposedResolution
            )
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to save briefing: ${e.message}", e))
        }
    }

    private fun BriefingEntity.toDomain(): Briefing {
        val parsedDate = try {
            LocalDate.parse(date)
        } catch (_: Exception) {
            LocalDate(2026, 10, 15)
        }
        val conflictsList = if (conflict_title != null) {
            listOf(
                ScheduleConflict(
                    id = "conflict_$id",
                    description = conflict_title,
                    proposedResolution = conflict_resolution ?: "Reschedule",
                    targetTime = com.yaan.core.common.datetime.nowInstant()
                )
            )
        } else emptyList()

        return Briefing(
            id = id,
            targetDate = parsedDate,
            summaryMarkdown = summary,
            cognitiveLoadScore = 65,
            commitments = emptyList(),
            conflicts = conflictsList
        )
    }
}
