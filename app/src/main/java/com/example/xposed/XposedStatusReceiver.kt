package com.example.xposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class XposedStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == "com.example.glyphisland.ACTION_MODULE_ACTIVE") {
            val prefs = context.getSharedPreferences("glyph_island_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("module_active", true).apply()
        }
    }
}
