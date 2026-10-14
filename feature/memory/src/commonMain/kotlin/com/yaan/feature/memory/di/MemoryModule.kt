package com.yaan.feature.memory.di

import com.yaan.feature.memory.MemoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val memoryModule = module {
    viewModel { MemoryViewModel(memoryRepository = get(), dispatchers = get()) }
}
