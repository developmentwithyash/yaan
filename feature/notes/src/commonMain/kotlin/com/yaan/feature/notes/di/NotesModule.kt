package com.yaan.feature.notes.di

import com.yaan.feature.notes.NotesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val notesModule = module {
    viewModel { NotesViewModel(noteRepository = get(), dispatchers = get()) }
}
