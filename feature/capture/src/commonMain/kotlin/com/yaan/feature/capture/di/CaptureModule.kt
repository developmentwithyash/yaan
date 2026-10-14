package com.yaan.feature.capture.di

import com.yaan.feature.capture.CaptureViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val captureModule = module {
    viewModel { CaptureViewModel(captureIntentUseCase = get(), dispatchers = get()) }
}
