package com.kidsafe.child.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AppUsage>)

    @Query("SELECT * FROM app_usage ORDER BY time DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<AppUsage>
}