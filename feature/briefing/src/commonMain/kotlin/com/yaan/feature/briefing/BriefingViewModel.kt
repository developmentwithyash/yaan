package com.yaan.feature.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.domain.usecase.GetDailyBriefingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BriefingViewModel(
    private val getDailyBriefingUseCase: GetDailyBriefingUseCase,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(BriefingState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        observeBriefing()
    }

    private fun observeBriefing() {
        viewModelScope.launch(dispatchers.main) {
            getDailyBriefingUseCase.observeLatest().collect { briefing ->
                _state.update {
                    it.copy(
                        briefing = briefing,
                        isLoading = false,
                        isStale = briefing?.isOfflineStale ?: false
                    )
                }
            }
        }
    }

    fun onIntent(intent: BriefingIntent) {
        when (intent) {
            is BriefingIntent.Refresh -> {
                // Trigger refresh check
            }
            is BriefingIntent.AdjustConflict -> {
                // Handled via action proposal
            }
        }
    }
}
