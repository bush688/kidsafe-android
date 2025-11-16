package com.kidsafe.child.profile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChildProfileDao {
    @Query("SELECT * FROM child_profile WHERE id = 1")
    suspend fun get(): ChildProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ChildProfile)
}