package com.kidsafe.child.lock

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LockConfigDao {
    @Query("SELECT * FROM lock_config WHERE id = 1")
    suspend fun get(): LockConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: LockConfig)
}