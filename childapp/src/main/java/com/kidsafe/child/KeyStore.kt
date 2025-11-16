package com.kidsafe.child

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object KeyStore {
    fun getDatabaseKey(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(context, "secure_prefs", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        var key = prefs.getString("db_key", null)
        if (key == null) {
            val k = java.util.UUID.randomUUID().toString().replace("-", "")
            prefs.edit().putString("db_key", k).apply()
            key = k
        }
        return key!!.toByteArray()
    }
}