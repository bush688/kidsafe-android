package com.kidsafe.probe.support

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class AssetTextRepository(private val appContext: Context) {
    suspend fun loadText(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            appContext.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrElse { e ->
            if (e is FileNotFoundException) null else null
        }
    }

    suspend fun loadLocalizedText(relativePath: String): String? {
        val lang = SupportLocale.currentTag()
        val localized = "support/$lang/$relativePath"
        return loadText(localized) ?: loadText("support/en/$relativePath")
    }

    suspend fun loadRawSupport(path: String): String? = loadText("support/$path")
}

