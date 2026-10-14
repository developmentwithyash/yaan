package com.yaan.core.domain.usecase

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Briefing
import com.yaan.core.domain.repository.BriefingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetDailyBriefingUseCase(
    private val briefingRepository: BriefingRepository,
) {
    fun observeLatest(): Flow<Briefing?> = briefingRepository.observeLatestBriefing()

    suspend fun getForDate(date: LocalDate): YaanResult<Briefing?> =
        briefingRepository.getBriefingForDate(date)
}
