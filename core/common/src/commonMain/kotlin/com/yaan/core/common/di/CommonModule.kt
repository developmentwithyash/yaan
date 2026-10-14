package com.yaan.core.common.di

import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.dispatcher.DefaultCoroutineDispatchers
import org.koin.dsl.module

val commonModule = module {
    single<CoroutineDispatchers> { DefaultCoroutineDispatchers() }
}
