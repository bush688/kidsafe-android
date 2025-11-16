package com.kidsafe.child

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kidsafe.child.db.AppUsage
import com.kidsafe.child.db.AppUsageDao
import com.kidsafe.child.rules.ScreenTimeRule
import com.kidsafe.child.rules.ScreenTimeRuleDao
import com.kidsafe.child.rules.TimeWindow
import com.kidsafe.child.rules.TimeWindowDao
import com.kidsafe.child.lock.LockConfig
import com.kidsafe.child.lock.LockConfigDao
import com.kidsafe.child.profile.ChildProfile
import com.kidsafe.child.profile.ChildProfileDao
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [AppUsage::class, ScreenTimeRule::class, LockConfig::class, TimeWindow::class, ChildProfile::class], version = 5)
abstract class SecureDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun screenTimeRuleDao(): ScreenTimeRuleDao
    abstract fun lockConfigDao(): LockConfigDao
    abstract fun timeWindowDao(): TimeWindowDao
    abstract fun childProfileDao(): ChildProfileDao

    companion object {
        @Volatile private var instance: SecureDatabase? = null
        fun get(context: Context): SecureDatabase {
            val i = instance
            if (i != null) return i
            SQLiteDatabase.loadLibs(context)
            val passphrase = KeyStore.getDatabaseKey(context)
            val factory = SupportFactory(passphrase)
            val db = Room.databaseBuilder(context, SecureDatabase::class.java, "secure.db").openHelperFactory(factory).fallbackToDestructiveMigration().build()
            instance = db
            return db
        }
    }
}