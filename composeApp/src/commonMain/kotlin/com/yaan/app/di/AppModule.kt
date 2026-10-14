package com.yaan.app.di

import com.yaan.core.common.di.commonModule
import com.yaan.core.data.di.dataModule
import com.yaan.core.domain.di.domainModule
import com.yaan.feature.actions.di.actionsModule
import com.yaan.feature.briefing.di.briefingModule
import com.yaan.feature.capture.di.captureModule
import com.yaan.feature.memory.di.memoryModule
import com.yaan.feature.notes.di.notesModule
import com.yaan.feature.reminders.di.remindersModule
import com.yaan.feature.tasks.di.tasksModule
import com.yaan.feature.voice.di.voiceModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

val appModules = listOf(
    commonModule,
    domainModule,
    dataModule,
    captureModule,
    remindersModule,
    notesModule,
    tasksModule,
    briefingModule,
    memoryModule,
    voiceModule,
    actionsModule,
)

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModules)
}
