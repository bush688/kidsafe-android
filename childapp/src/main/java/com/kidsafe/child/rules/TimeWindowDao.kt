package com.kidsafe.child.rules

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TimeWindowDao {
    @Query("SELECT * FROM time_window WHERE id = 1")
    suspend fun get(): TimeWindow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: TimeWindow)
}