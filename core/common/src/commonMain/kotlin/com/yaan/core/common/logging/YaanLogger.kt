package com.yaan.core.common.logging

import io.github.aakira.napier.Napier

/**
 * Universal logging façade for YAAN subsystems.
 */
object YaanLogger {
    fun d(tag: String, message: () -> String) {
        Napier.d(tag = tag, message = message)
    }

    fun i(tag: String, message: () -> String) {
        Napier.i(tag = tag, message = message)
    }

    fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        Napier.w(tag = tag, throwable = throwable, message = message)
    }

    fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        Napier.e(tag = tag, throwable = throwable, message = message)
    }
}
