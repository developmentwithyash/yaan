package com.yaan.app.desktop

import java.util.prefs.Preferences

/**
 * JVM implementation of DesktopPreferencesStore backed by java.util.prefs.Preferences.
 */
class JvmDesktopPreferencesStore : DesktopPreferencesStore {
    private val prefs: Preferences = Preferences.userNodeForPackage(DesktopWindowStateManager::class.java)

    override fun getInt(key: String, def: Int): Int {
        return prefs.getInt(key, def)
    }

    override fun putInt(key: String, value: Int) {
        prefs.putInt(key, value)
    }
}
