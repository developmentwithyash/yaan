package com.yaan.app.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopWindowStateManagerTest {

    private class InMemoryPreferencesStore : DesktopPreferencesStore {
        private val map = mutableMapOf<String, Int>()

        override fun getInt(key: String, def: Int): Int = map[key] ?: def
        override fun putInt(key: String, value: Int) {
            map[key] = value
        }
    }

    @Test
    fun testDefaultWindowStateLoadsWhenStoreIsEmpty() {
        val store = InMemoryPreferencesStore()
        val manager = DesktopWindowStateManager(store)

        val state = manager.loadWindowState()
        assertEquals(440, state.widthDp)
        assertEquals(880, state.heightDp)
        assertNull(state.positionXDp)
        assertNull(state.positionYDp)
    }

    @Test
    fun testClampingToMinimumDimensionsWhenSavedValuesTooSmall() {
        val store = InMemoryPreferencesStore()
        store.putInt(DesktopWindowStateManager.KEY_WIDTH, 200)
        store.putInt(DesktopWindowStateManager.KEY_HEIGHT, 400)

        val manager = DesktopWindowStateManager(store)
        val state = manager.loadWindowState()

        assertEquals(DesktopWindowStateManager.MIN_WIDTH_DP, state.widthDp)
        assertEquals(DesktopWindowStateManager.MIN_HEIGHT_DP, state.heightDp)
    }

    @Test
    fun testCustomWindowDimensionsPersistedAndReloaded() {
        val store = InMemoryPreferencesStore()
        val manager = DesktopWindowStateManager(store)

        manager.saveWindowState(
            widthDp = 700,
            heightDp = 1000,
            positionXDp = 120,
            positionYDp = 80
        )

        val reloaded = manager.loadWindowState()
        assertEquals(700, reloaded.widthDp)
        assertEquals(1000, reloaded.heightDp)
        assertEquals(120, reloaded.positionXDp)
        assertEquals(80, reloaded.positionYDp)
    }

    @Test
    fun testClampingWhenSavingUndersizedDimensions() {
        val store = InMemoryPreferencesStore()
        val manager = DesktopWindowStateManager(store)

        manager.saveWindowState(
            widthDp = 150,
            heightDp = 250
        )

        val reloaded = manager.loadWindowState()
        assertEquals(DesktopWindowStateManager.MIN_WIDTH_DP, reloaded.widthDp)
        assertEquals(DesktopWindowStateManager.MIN_HEIGHT_DP, reloaded.heightDp)
    }
}
