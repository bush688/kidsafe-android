package com.kidsafe.child.rules

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScreenTimeRuleDao {
    @Query("SELECT * FROM screen_time_rule WHERE id = 1")
    suspend fun get(): ScreenTimeRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: ScreenTimeRule)
}