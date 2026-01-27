package com.kidsafe.probe.its90

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream

class Its90Repository(private val context: Context) {
    private val tables: Map<Int, Table1D> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadTables()
    }

    fun thermocoupleTable(type: ThermocoupleType): Its90Lookup {
        val t = tables[type.tableId] ?: error("Missing table for thermocouple ${type.displayName}")
        return Its90Lookup(t)
    }

    fun rtdTable(type: RtdType): Its90Lookup {
        val t = tables[type.tableId] ?: error("Missing table for RTD ${type.displayName}")
        return Its90Lookup(t)
    }

    private fun loadTables(): Map<Int, Table1D> {
        val dbFile = ensureDbOnDisk()
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

        val groups = linkedMapOf<Int, MutableList<Pair<Double, Double>>>()
        db.rawQuery("select t,c,v from typeValue order by t,c", null).use { cursor ->
            val idxT = 0
            val idxC = 1
            val idxV = 2
            while (cursor.moveToNext()) {
                val t = cursor.getInt(idxT)
                val c = cursor.getDouble(idxC)
                val v = cursor.getDouble(idxV)
                groups.getOrPut(t) { ArrayList() }.add(c to v)
            }
        }
        db.close()

        return groups.mapValues { (_, points) ->
            val xs = DoubleArray(points.size)
            val ys = DoubleArray(points.size)
            for (i in points.indices) {
                val (x, y) = points[i]
                xs[i] = x
                ys[i] = y
            }
            Table1D(xs, ys)
        }
    }

    private fun ensureDbOnDisk(): File {
        val dir = File(context.noBackupFilesDir, "its90")
        if (!dir.exists()) dir.mkdirs()
        val out = File(dir, "sadi_its90.sqlite")
        if (out.exists() && out.length() > 0) return out
        context.assets.open("its90/sadi_its90.sqlite").use { input ->
            FileOutputStream(out).use { fos ->
                input.copyTo(fos)
            }
        }
        return out
    }
}

class Its90Lookup internal constructor(private val table: Table1D) {
    val minTemperatureC: Double get() = table.minX
    val maxTemperatureC: Double get() = table.maxX
    val minValue: Double get() = table.minY
    val maxValue: Double get() = table.maxY

    fun temperatureToValue(tC: Double): Double = table.forward(tC)
    fun valueToTemperature(v: Double): Double = table.inverse(v)
}

