package com.yaan.feature.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActionsViewModel(
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(ActionsState())
    val state = _state.asStateFlow()

    fun onIntent(intent: ActionsIntent) {
        when (intent) {
            is ActionsIntent.ApproveAction -> {
                viewModelScope.launch(dispatchers.main) {
                    _state.update {
                        val updated = it.proposals.map { p ->
                            if (p.id == intent.id) p.copy(isExecuted = true) else p
                        }
                        it.copy(proposals = updated)
                    }
                }
            }
            is ActionsIntent.ModifyAction -> {
                // Handled in UI
            }
        }
    }
}
