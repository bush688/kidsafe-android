package com.kidsafe.child.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kidsafe.child.AppUsageEvent

@Entity(tableName = "app_usage")
data class AppUsage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val type: Int,
    val time: Long
) {
    companion object {
        fun fromEvent(e: AppUsageEvent) = AppUsage(packageName = e.packageName, type = e.type, time = e.time)
    }
}