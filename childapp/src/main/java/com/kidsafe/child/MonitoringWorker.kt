package com.kidsafe.child

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kidsafe.child.db.AppUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MonitoringWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val events = UsageMonitor.collectToday(applicationContext)
        SecureDatabase.get(applicationContext).appUsageDao().insertAll(events.map { AppUsage.fromEvent(it) })
        return Result.success()
    }
}