package com.example.xposed

import android.content.Context
import android.content.Intent
import android.provider.Settings

object XposedStatus {
    @JvmStatic
    fun isModuleActive(): Boolean {
        return false
    }

    @JvmStatic
    fun isModuleActive(context: Context?): Boolean {
        if (isModuleActive()) {
            return true
        }
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("glyph_island_prefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("module_active", false)) {
                    return true
                }
                return Settings.Global.getInt(context.contentResolver, "glyph_island_module_active", 0) == 1
            } catch (t: Throwable) {
                // Fallback
            }
        }
        return false
    }

    @JvmStatic
    fun sendSimulationBroadcast(context: Context?, state: String, sender: String, text: String, appName: String) {
        if (context == null) return
        try {
            val intent = Intent("com.example.glyphisland.ACTION_SIMULATE_STATE").apply {
                putExtra("state", state)
                putExtra("sender", sender)
                putExtra("text", text)
                putExtra("app", appName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // Ignored
        }
    }
}
