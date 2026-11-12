package com.edgeback.customizer

import android.content.Context
import android.provider.Settings
import kotlin.math.roundToInt

object ConfigSync {
    private const val KEY = "ebc_cfg"

    class Colors(val bg: Int, val fg: Int, val bg2: Int)

    fun value(p: Prefs, c: Colors): String {
        fun b(v: Boolean) = if (v) 1 else 0
        return listOf(
            b(p.enableHooks.value), b(p.haptic.value), p.zonePercent.value.roundToInt(),
            b(p.pillLook.value), p.pillAlpha.value.roundToInt(), (p.pillScale.value * 100f).roundToInt(),
            p.pillRound.value.roundToInt(), c.bg, c.fg, b(p.pillGradient.value), c.bg2,
            p.gradAngle.value.roundToInt(), p.pillW.value.roundToInt(), p.pillH.value.roundToInt(),
            p.arrowSize.value.roundToInt(), (p.arrowStroke.value * 10f).roundToInt(),
            p.glow.value.roundToInt(), p.offsetY.value.roundToInt(),
            b(p.edgeLeft.value), b(p.edgeRight.value), p.edgeWidth.value.roundToInt(),
            (p.outline.value * 10f).roundToInt(),
        ).joinToString(",")
    }

    fun push(ctx: Context, v: String, allowRoot: Boolean): Boolean {
        return try {
            Settings.Global.putString(ctx.contentResolver, KEY, v)
        } catch (se: SecurityException) {
            if (allowRoot) {
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
