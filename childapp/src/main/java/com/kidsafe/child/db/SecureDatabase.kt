package com.kidsafe.child

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kidsafe.child.db.AppUsage
import com.kidsafe.child.db.AppUsageDao
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [AppUsage::class], version = 1)
abstract class SecureDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao

    companion object {
        @Volatile private var instance: SecureDatabase? = null
        fun get(context: Context): SecureDatabase {
            val i = instance
            if (i != null) return i
            SQLiteDatabase.loadLibs(context)
            val passphrase = KeyStore.getDatabaseKey(context)
            val factory = SupportFactory(passphrase)
            val db = Room.databaseBuilder(context, SecureDatabase::class.java, "secure.db").openHelperFactory(factory).build()
            instance = db
            return db
        }
    }
}