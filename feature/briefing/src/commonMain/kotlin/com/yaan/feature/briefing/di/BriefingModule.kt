package com.yaan.feature.briefing.di

import com.yaan.feature.briefing.BriefingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val briefingModule = module {
    viewModel { BriefingViewModel(getDailyBriefingUseCase = get(), dispatchers = get()) }
}
