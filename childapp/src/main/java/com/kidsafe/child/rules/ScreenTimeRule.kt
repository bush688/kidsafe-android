package com.kidsafe.child.rules

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_time_rule")
data class ScreenTimeRule(
    @PrimaryKey val id: Int = 1,
    val dailyLimitMinutes: Int = 60
)