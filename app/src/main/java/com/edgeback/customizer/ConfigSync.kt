package com.edgeback.customizer

import android.content.Context
import android.provider.Settings
import kotlin.math.roundToInt

/**
 * Sends the live settings to System UI through Settings.Global "ebc_cfg" = "hooks,haptic,zone".
 * Needs WRITE_SECURE_SETTINGS; if it is missing and root was already granted, grants it once via root.
 */
object ConfigSync {
    private const val KEY = "ebc_cfg"

    fun value(p: Prefs): String {
        val hooks = if (p.enableHooks.value) 1 else 0
        val haptic = if (p.haptic.value) 1 else 0
        return "$hooks,$haptic,${p.zonePercent.value.roundToInt()}"
    }

    /** Blocking. Returns true when the value reached Settings.Global. */
    fun push(ctx: Context, p: Prefs): Boolean {
        val v = value(p)
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
