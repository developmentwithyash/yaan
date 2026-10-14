package com.yaan.feature.reminders.di

import com.yaan.feature.reminders.RemindersViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val remindersModule = module {
    viewModel { RemindersViewModel(reminderRepository = get(), dispatchers = get()) }
}
