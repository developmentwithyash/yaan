package com.yaan.core.data.di

import com.yaan.core.data.repository.InMemoryBriefingRepository
import com.yaan.core.data.repository.InMemoryMemoryRepository
import com.yaan.core.data.repository.InMemoryNoteRepository
import com.yaan.core.data.repository.InMemoryReminderRepository
import com.yaan.core.data.repository.InMemorySyncRepository
import com.yaan.core.data.repository.InMemoryTaskRepository
import com.yaan.core.domain.repository.BriefingRepository
import com.yaan.core.domain.repository.MemoryRepository
import com.yaan.core.domain.repository.NoteRepository
import com.yaan.core.domain.repository.ReminderRepository
import com.yaan.core.domain.repository.SyncRepository
import com.yaan.core.domain.repository.TaskRepository
import org.koin.dsl.module

val dataModule = module {
    single<TaskRepository> { InMemoryTaskRepository() }
    single<NoteRepository> { InMemoryNoteRepository() }
    single<ReminderRepository> { InMemoryReminderRepository() }
    single<BriefingRepository> { InMemoryBriefingRepository() }
    single<MemoryRepository> { InMemoryMemoryRepository() }
    single<SyncRepository> { InMemorySyncRepository() }
}
