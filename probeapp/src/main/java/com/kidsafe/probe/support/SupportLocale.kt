package com.kidsafe.probe.support

import java.util.Locale

object SupportLocale {
    fun currentTag(): String {
        val lang = Locale.getDefault().language.lowercase(Locale.US)
        return when (lang) {
            "zh" -> "zh"
            "en" -> "en"
            "es" -> "es"
            else -> "en"
        }
    }
}

