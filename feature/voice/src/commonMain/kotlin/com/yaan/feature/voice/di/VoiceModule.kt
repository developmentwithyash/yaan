package com.yaan.feature.voice.di

import com.yaan.feature.voice.VoiceViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val voiceModule = module {
    viewModel { VoiceViewModel(dispatchers = get()) }
}
