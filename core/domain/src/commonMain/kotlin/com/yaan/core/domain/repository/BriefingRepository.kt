package com.yaan.core.domain.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Briefing
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface BriefingRepository {
    fun observeLatestBriefing(): Flow<Briefing?>
    suspend fun getBriefingForDate(date: LocalDate): YaanResult<Briefing?>
    suspend fun saveBriefing(briefing: Briefing): YaanResult<Unit>
}
