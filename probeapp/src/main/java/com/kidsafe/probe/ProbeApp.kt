package com.kidsafe.probe

import android.app.Application
import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProbeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { CrashReporter.writeCrash(this, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }
}

object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"

    private fun crashFile(context: Context): File = File(context.filesDir, FILE_NAME)

    fun readCrash(context: Context): String? {
        val file = crashFile(context)
        if (!file.exists()) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    fun clearCrash(context: Context) {
        runCatching { crashFile(context).delete() }
    }

    fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val text = buildString {
            appendLine("time=$ts")
            appendLine("thread=${thread.name}")
            appendLine("${throwable::class.java.name}: ${throwable.message}")
            appendLine()
            appendLine(throwable.stackTraceToString())
        }
        crashFile(context).writeText(text, Charsets.UTF_8)
    }
}
