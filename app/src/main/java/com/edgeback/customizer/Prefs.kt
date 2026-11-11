package com.edgeback.customizer

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

abstract class BasePref {
    abstract val key: String
    abstract fun reload()
    abstract fun write(o: JSONObject)
    abstract fun read(o: JSONObject)
}

class BoolPref(
    private val sp: SharedPreferences,
    override val key: String,
    private val default: Boolean,
) : BasePref() {
    private var state by mutableStateOf(sp.getBoolean(key, default))
    var value: Boolean
        get() = state
        set(v) {
            state = v
            sp.edit().putBoolean(key, v).apply()
        }

    override fun reload() { state = sp.getBoolean(key, default) }
    override fun write(o: JSONObject) { o.put(key, value) }
    override fun read(o: JSONObject) { if (o.has(key)) value = o.getBoolean(key) }
}

class IntPref(
    private val sp: SharedPreferences,
    override val key: String,
    private val default: Int,
) : BasePref() {
    private var state by mutableStateOf(sp.getInt(key, default))
    var value: Int
        get() = state
        set(v) {
            state = v
            sp.edit().putInt(key, v).apply()
        }

    override fun reload() { state = sp.getInt(key, default) }
    override fun write(o: JSONObject) { o.put(key, value) }
    override fun read(o: JSONObject) { if (o.has(key)) value = o.getInt(key) }
}

class FloatPref(
    private val sp: SharedPreferences,
    override val key: String,
    private val default: Float,
) : BasePref() {
    private var state by mutableStateOf(sp.getFloat(key, default))
    var value: Float
        get() = state
        set(v) {
            state = v
            sp.edit().putFloat(key, v).apply()
        }

    override fun reload() { state = sp.getFloat(key, default) }
    override fun write(o: JSONObject) { o.put(key, value.toDouble()) }
    override fun read(o: JSONObject) { if (o.has(key)) value = o.getDouble(key).toFloat() }
}

class StringPref(
    private val sp: SharedPreferences,
    override val key: String,
    private val default: String,
) : BasePref() {
    private var state by mutableStateOf(sp.getString(key, default) ?: default)
    var value: String
        get() = state
        set(v) {
            state = v
            sp.edit().putString(key, v).apply()
        }

    override fun reload() { state = sp.getString(key, default) ?: default }
    override fun write(o: JSONObject) { o.put(key, value) }
    override fun read(o: JSONObject) { if (o.has(key)) value = o.getString(key) }
}

/** All app-side settings. Phase 1 will mirror the pill/zone/haptic/hook values to the module. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ebc_prefs", Context.MODE_PRIVATE)
    private val backedUp = mutableListOf<BasePref>()

    private fun <T : BasePref> reg(p: T): T {
        backedUp += p
        return p
    }

    // appearance
    val themeMode = reg(IntPref(sp, "theme_mode", 0)) // 0 system, 1 light, 2 dark
    val monet = reg(BoolPref(sp, "monet", true))
    val accentHue = reg(FloatPref(sp, "accent_hue", 246f))
    val amoled = reg(BoolPref(sp, "amoled", false))

    // bottom bar
    val dockFloating = reg(BoolPref(sp, "dock_floating", true))
    val dockWidth = reg(FloatPref(sp, "dock_width", 50f))       // % of screen width
    val dockHeight = reg(FloatPref(sp, "dock_height", 61f))     // dp
    val dockOpacity = reg(FloatPref(sp, "dock_opacity", 100f))  // %
    val dockTextSize = reg(FloatPref(sp, "dock_text", 9f))      // sp
    val dockBottom = reg(FloatPref(sp, "dock_bottom", 5f))      // dp
    val dockHide = reg(BoolPref(sp, "dock_hide", true))
    val dockLabels = reg(BoolPref(sp, "dock_labels", false))

    // customization (not yet applied to the system gesture)
    val pillLook = reg(BoolPref(sp, "pill_look", false))
    val pillGradient = reg(BoolPref(sp, "pill_gradient", false))
    val pillAlpha = reg(FloatPref(sp, "pill_alpha", 100f))      // %
    val pillRound = reg(FloatPref(sp, "pill_round", 100f))      // % (100 = full circle)
    val pillScale = reg(FloatPref(sp, "pill_scale", 1f))
    val zonePercent = reg(FloatPref(sp, "zone_percent", 100f))
    val haptic = reg(BoolPref(sp, "haptic", true))

    // module
    val enableHooks = reg(BoolPref(sp, "enable_hooks", true))
    val debugLog = reg(BoolPref(sp, "debug_log", false))
    val hideIcon = reg(BoolPref(sp, "hide_icon", false))

    // cached root info (not part of backups)
    val rootChecked = BoolPref(sp, "root_checked", false)
    val rootSolution = StringPref(sp, "root_solution", "")
    val zygisk = StringPref(sp, "zygisk", "")

    fun exportJson(): String {
        val o = JSONObject()
        backedUp.forEach { it.write(o) }
        return o.toString(2)
    }

    fun importJson(text: String): Boolean = try {
        val o = JSONObject(text)
        backedUp.forEach { it.read(o) }
        true
    } catch (t: Throwable) {
        false
    }
}
