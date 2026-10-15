package com.yaan.core.data.di

import com.yaan.core.data.crypto.AndroidVaultKeyManager
import com.yaan.core.data.crypto.VaultKeyManager
import com.yaan.core.data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { DatabaseDriverFactory(context = get()) }
    single<VaultKeyManager> { AndroidVaultKeyManager(context = get()) }
}
