package com.yaan.core.data.di

import com.yaan.core.data.crypto.IosVaultKeyManager
import com.yaan.core.data.crypto.VaultKeyManager
import com.yaan.core.data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { DatabaseDriverFactory() }
    single<VaultKeyManager> { IosVaultKeyManager() }
}
