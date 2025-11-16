package com.kidsafe.child.rules

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_window")
data class TimeWindow(
    @PrimaryKey val id: Int = 1,
    val startMinutes: Int = 480,
    val endMinutes: Int = 1200
)