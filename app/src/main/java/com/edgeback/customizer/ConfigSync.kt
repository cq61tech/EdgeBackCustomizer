package com.edgeback.customizer

import android.content.Context
import android.provider.Settings
import kotlin.math.roundToInt

/**
 * Sends the live settings to System UI through Settings.Global "ebc_cfg" = "hooks,haptic,zone,look,alpha,scale100,round,bg,fg,grad,bg2".
 * Needs WRITE_SECURE_SETTINGS; if it is missing and root was already granted, grants it once via root.
 */
object ConfigSync {
    private const val KEY = "ebc_cfg"

    /** Colors are ARGB ints taken from the app theme (so the real pill matches the app). */
    class Colors(val bg: Int, val fg: Int, val bg2: Int)

    fun value(p: Prefs, c: Colors): String {
        val hooks = if (p.enableHooks.value) 1 else 0
        val haptic = if (p.haptic.value) 1 else 0
        val look = if (p.pillLook.value) 1 else 0
        val grad = if (p.pillGradient.value) 1 else 0
        return listOf(
            hooks, haptic, p.zonePercent.value.roundToInt(),
            look, p.pillAlpha.value.roundToInt(), (p.pillScale.value * 100f).roundToInt(),
            p.pillRound.value.roundToInt(), c.bg, c.fg, grad, c.bg2,
        ).joinToString(",")
    }

    /** Blocking. Returns true when the value reached Settings.Global. */
    fun push(ctx: Context, p: Prefs, c: Colors): Boolean {
        val v = value(p, c)
        return try {
            Settings.Global.putString(ctx.contentResolver, KEY, v)
        } catch (se: SecurityException) {
            if (p.rootChecked.value && p.rootSolution.value.isNotBlank()) {
                RootInfo.exec(
                    "pm grant ${ctx.packageName} android.permission.WRITE_SECURE_SETTINGS; " +
                        "settings put global $KEY $v",
                )
            } else {
                false
            }
        } catch (t: Throwable) {
            false
        }
    }
}
