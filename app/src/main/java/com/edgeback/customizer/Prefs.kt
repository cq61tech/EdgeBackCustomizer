package com.edgeback.customizer

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class BoolPref(private val sp: SharedPreferences, private val key: String, default: Boolean) {
    private var state by mutableStateOf(sp.getBoolean(key, default))
    var value: Boolean
        get() = state
        set(v) {
            state = v
            sp.edit().putBoolean(key, v).apply()
        }
}

class IntPref(private val sp: SharedPreferences, private val key: String, default: Int) {
    private var state by mutableStateOf(sp.getInt(key, default))
    var value: Int
        get() = state
        set(v) {
            state = v
            sp.edit().putInt(key, v).apply()
        }
}

class FloatPref(private val sp: SharedPreferences, private val key: String, default: Float) {
    private var state by mutableStateOf(sp.getFloat(key, default))
    var value: Float
        get() = state
        set(v) {
            state = v
            sp.edit().putFloat(key, v).apply()
        }
}

class StringPref(private val sp: SharedPreferences, private val key: String, default: String) {
    private var state by mutableStateOf(sp.getString(key, default) ?: default)
    var value: String
        get() = state
        set(v) {
            state = v
            sp.edit().putString(key, v).apply()
        }
}

/** All app-side settings. Phase 1 will mirror the pill/zone/haptic values to the module. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ebc_prefs", Context.MODE_PRIVATE)

    // app
    val themeMode = IntPref(sp, "theme_mode", 0) // 0 system, 1 light, 2 dark
    val monet = BoolPref(sp, "monet", true)
    val dockFloating = BoolPref(sp, "dock_floating", true)
    val dockLabels = BoolPref(sp, "dock_labels", true)

    // customization (not yet applied to the system gesture)
    val pillAlpha = FloatPref(sp, "pill_alpha", 0.9f)
    val pillRadius = FloatPref(sp, "pill_radius", 24f)
    val pillScale = FloatPref(sp, "pill_scale", 1f)
    val zonePercent = FloatPref(sp, "zone_percent", 100f)
    val haptic = BoolPref(sp, "haptic", true)

    // cached root info
    val rootChecked = BoolPref(sp, "root_checked", false)
    val rootSolution = StringPref(sp, "root_solution", "")
    val zygisk = StringPref(sp, "zygisk", "")
}
