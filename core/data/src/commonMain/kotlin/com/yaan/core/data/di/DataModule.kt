package com.yaan.core.data.di

import com.yaan.core.data.crypto.AesGcmYaanCipher
import com.yaan.core.data.crypto.YaanCipher
import com.yaan.core.data.db.DatabaseDriverFactory
import com.yaan.core.data.db.VaultDatabaseInitializer
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.data.repository.InMemorySyncRepository
import com.yaan.core.data.repository.SqlDelightBriefingRepository
import com.yaan.core.data.repository.SqlDelightMemoryRepository
import com.yaan.core.data.repository.SqlDelightNoteRepository
import com.yaan.core.data.repository.SqlDelightReminderRepository
import com.yaan.core.data.repository.SqlDelightTaskRepository
import com.yaan.core.domain.repository.BriefingRepository
import com.yaan.core.domain.repository.MemoryRepository
import com.yaan.core.domain.repository.NoteRepository
import com.yaan.core.domain.repository.ReminderRepository
import com.yaan.core.domain.repository.SyncRepository
import com.yaan.core.domain.repository.TaskRepository
import org.koin.dsl.module

val dataModule = module {
    includes(platformDataModule)

    single<YaanCipher> { AesGcmYaanCipher() }

    single<YaanDatabase> {
        val driverFactory: DatabaseDriverFactory = get()
        val driver = driverFactory.createDriver()
        val db = YaanDatabase(driver)
        VaultDatabaseInitializer.seedIfEmpty(db)
        db
    }

    single<TaskRepository> { SqlDelightTaskRepository(database = get(), dispatchers = get()) }
    single<NoteRepository> {
        SqlDelightNoteRepository(
            database = get(),
            dispatchers = get(),
            cipher = get(),
            keyManager = get()
        )
    }
    single<ReminderRepository> { SqlDelightReminderRepository(database = get(), dispatchers = get()) }
    single<BriefingRepository> { SqlDelightBriefingRepository(database = get(), dispatchers = get()) }
    single<MemoryRepository> { SqlDelightMemoryRepository(database = get(), dispatchers = get()) }
    single<SyncRepository> { InMemorySyncRepository() }
}
