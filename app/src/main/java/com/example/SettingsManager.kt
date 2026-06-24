package com.example

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("glyph_island_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_DOUBLE_TAP_ACTION = "double_tap_action"
        const val KEY_ISLAND_WIDTH = "island_width"
        const val KEY_ISLAND_HEIGHT = "island_height"
        const val KEY_ISLAND_Y_OFFSET = "island_y_offset"
        const val KEY_ISLAND_CORNER_RADIUS = "island_corner_radius"
        const val KEY_BRIGHTNESS = "brightness"
        const val KEY_VOLUME = "volume"
        const val KEY_ENABLE_OVERLAY = "enable_overlay"
        const val KEY_ENABLE_GLYPHS = "enable_glyphs"
        const val KEY_ZYGISK_FAST_MODE = "zygisk_fast_mode"

        const val ACTION_OPEN_APP = "Open App"
        const val ACTION_SCREENSHOT = "Screenshot"
        const val ACTION_FLASHLIGHT = "Flashlight"
        const val ACTION_LAST_APP = "Last App"
    }

    var doubleTapAction: String
        get() = prefs.getString(KEY_DOUBLE_TAP_ACTION, ACTION_OPEN_APP) ?: ACTION_OPEN_APP
        set(value) = prefs.edit().putString(KEY_DOUBLE_TAP_ACTION, value).apply()

    var islandWidth: Float
        get() = prefs.getFloat(KEY_ISLAND_WIDTH, 180f)
        set(value) = prefs.edit().putFloat(KEY_ISLAND_WIDTH, value).apply()

    var islandHeight: Float
        get() = prefs.getFloat(KEY_ISLAND_HEIGHT, 40f)
        set(value) = prefs.edit().putFloat(KEY_ISLAND_HEIGHT, value).apply()

    var islandYOffset: Float
        get() = prefs.getFloat(KEY_ISLAND_Y_OFFSET, 18f)
        set(value) = prefs.edit().putFloat(KEY_ISLAND_Y_OFFSET, value).apply()

    var islandCornerRadius: Float
        get() = prefs.getFloat(KEY_ISLAND_CORNER_RADIUS, 20f)
        set(value) = prefs.edit().putFloat(KEY_ISLAND_CORNER_RADIUS, value).apply()

    var brightness: Int
        get() = prefs.getInt(KEY_BRIGHTNESS, 70)
        set(value) = prefs.edit().putInt(KEY_BRIGHTNESS, value).apply()

    var volume: Int
        get() = prefs.getInt(KEY_VOLUME, 50)
        set(value) = prefs.edit().putInt(KEY_VOLUME, value).apply()

    var enableOverlay: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_OVERLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_OVERLAY, value).apply()

    var enableGlyphs: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_GLYPHS, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_GLYPHS, value).apply()

    var zygiskFastMode: Boolean
        get() = prefs.getBoolean(KEY_ZYGISK_FAST_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_ZYGISK_FAST_MODE, value).apply()
}
