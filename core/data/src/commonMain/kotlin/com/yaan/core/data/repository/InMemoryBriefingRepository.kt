package com.yaan.core.data.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Briefing
import com.yaan.core.domain.repository.BriefingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

class InMemoryBriefingRepository : BriefingRepository {
    private val mutex = Mutex()
    private val briefingFlow = MutableStateFlow<Briefing?>(null)

    override fun observeLatestBriefing(): Flow<Briefing?> = briefingFlow.asStateFlow()

    override suspend fun getBriefingForDate(date: LocalDate): YaanResult<Briefing?> = mutex.withLock {
        val current = briefingFlow.value
        if (current?.targetDate == date) {
            YaanResult.Success(current)
        } else {
            YaanResult.Success(null)
        }
    }

    override suspend fun saveBriefing(briefing: Briefing): YaanResult<Unit> = mutex.withLock {
        briefingFlow.value = briefing
        YaanResult.Success(Unit)
    }
}
