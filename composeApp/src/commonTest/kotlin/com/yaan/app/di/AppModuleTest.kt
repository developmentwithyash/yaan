package com.yaan.app.di

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppModuleTest {

    @Test
    fun testAppModulesRegistration() {
        assertEquals(11, appModules.size)
        // Verify core modules
        assertTrue(appModules.contains(com.yaan.core.common.di.commonModule))
        assertTrue(appModules.contains(com.yaan.core.domain.di.domainModule))
        assertTrue(appModules.contains(com.yaan.core.data.di.dataModule))
        // Verify feature modules
        assertTrue(appModules.contains(com.yaan.feature.capture.di.captureModule))
        assertTrue(appModules.contains(com.yaan.feature.reminders.di.remindersModule))
        assertTrue(appModules.contains(com.yaan.feature.notes.di.notesModule))
        assertTrue(appModules.contains(com.yaan.feature.tasks.di.tasksModule))
        assertTrue(appModules.contains(com.yaan.feature.briefing.di.briefingModule))
        assertTrue(appModules.contains(com.yaan.feature.memory.di.memoryModule))
        assertTrue(appModules.contains(com.yaan.feature.voice.di.voiceModule))
        assertTrue(appModules.contains(com.yaan.feature.actions.di.actionsModule))
    }
}
