package com.yaan.core.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform expect declaration for SQLite driver creation.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
