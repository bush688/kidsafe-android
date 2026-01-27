package com.kidsafe.probe.modules

import android.content.Context
import android.util.Log

object ModuleRegistry {
    private const val ASSET_MODULE_LIST = "modules.txt"

    fun loadModules(context: Context): List<FeatureModule> {
        val fromAssets = runCatching { loadFromAssets(context) }.getOrNull()
        if (!fromAssets.isNullOrEmpty()) return fromAssets
        return BuiltInModules.all()
    }

    private fun loadFromAssets(context: Context): List<FeatureModule> {
        val assetManager = context.assets
        val raw = assetManager.open(ASSET_MODULE_LIST).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val classNames = raw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        val modules = mutableListOf<FeatureModule>()
        classNames.forEach { name ->
            val module = runCatching { instantiate(name) }.getOrElse { e ->
                Log.e("ModuleRegistry", "Load module failed: $name", e)
                null
            }
            if (module != null) modules.add(module)
        }
        return modules
    }

    private fun instantiate(className: String): FeatureModule {
        val cls = Class.forName(className)
        val instance = runCatching { cls.getField("INSTANCE").get(null) }.getOrNull()
            ?: runCatching { cls.getDeclaredConstructor().newInstance() }.getOrNull()
            ?: throw IllegalStateException("No INSTANCE or no-arg constructor: $className")

        return instance as? FeatureModule
            ?: throw IllegalStateException("Class is not FeatureModule: $className")
    }
}
