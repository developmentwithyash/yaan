package com.yaan.feature.tasks.di

import com.yaan.feature.tasks.TasksViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tasksModule = module {
    viewModel { TasksViewModel(resolveTaskChecklistUseCase = get(), dispatchers = get()) }
}
