package com.yaan.core.domain.di

import com.yaan.core.domain.ai.ActionGuardrailGate
import com.yaan.core.domain.ai.AiIntentClassifier
import com.yaan.core.domain.ai.HybridEdgeIntentClassifier
import com.yaan.core.domain.ai.StrictActionGuardrailGate
import com.yaan.core.domain.usecase.CaptureIntentUseCase
import com.yaan.core.domain.usecase.GetDailyBriefingUseCase
import com.yaan.core.domain.usecase.QueryPersonalVaultUseCase
import com.yaan.core.domain.usecase.ResolveTaskChecklistUseCase
import org.koin.dsl.module

val domainModule = module {
    single<AiIntentClassifier> { HybridEdgeIntentClassifier() }
    single<ActionGuardrailGate> { StrictActionGuardrailGate() }

    factory {
        CaptureIntentUseCase(
            taskRepository = get(),
            noteRepository = get(),
            classifier = get()
        )
    }
    factory { GetDailyBriefingUseCase(briefingRepository = get()) }
    factory { ResolveTaskChecklistUseCase(taskRepository = get()) }
    factory { QueryPersonalVaultUseCase(noteRepository = get()) }
}
