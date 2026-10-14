package com.yaan.feature.actions.di

import com.yaan.feature.actions.ActionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val actionsModule = module {
    viewModel { ActionsViewModel(dispatchers = get()) }
}
