package com.goosesdream.golaping.common.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.logger(): Logger {
    return if (T::class.isCompanion) {
        LoggerFactory.getLogger(T::class.java.enclosingClass)
    } else {
        LoggerFactory.getLogger(T::class.java)
    }
}
