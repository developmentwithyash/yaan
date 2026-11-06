package com.yaan.core.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val yaanDir = File(System.getProperty("user.home"), ".yaan")
        if (!yaanDir.exists()) yaanDir.mkdirs()
        val dbFile = File(yaanDir, "yaan_vault.db")
        val isNew = !dbFile.exists()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNew) {
            YaanDatabase.Schema.create(driver)
        }
        return driver
    }
}
