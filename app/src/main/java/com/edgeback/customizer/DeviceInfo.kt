package com.edgeback.customizer

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.TimeUnit

class RootResult(val solution: String, val zygisk: String)

/** Shows or hides the launcher icon (the alias declared in the manifest). */
fun setLauncherIconHidden(ctx: Context, hidden: Boolean) {
    ctx.packageManager.setComponentEnabledSetting(
        ComponentName(ctx.packageName, "com.edgeback.customizer.LauncherAlias"),
        if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP,
    )
}

object RootInfo {
    /** Blocking. Runs one command as root; true when it exited with 0. */
    fun exec(cmd: String): Boolean = try {
        val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        p.inputStream.bufferedReader().readText()
        if (!p.waitFor(8, TimeUnit.SECONDS)) {
            p.destroy()
            false
        } else {
            p.exitValue() == 0
        }
    } catch (t: Throwable) {
        false
    }

    private val SCRIPT = """
echo "PKGS:${'$'}(pm list packages 2>/dev/null | grep -E 'kernelsu|ksunext|apatch|magisk' | tr '\n' ' ')"
echo "KSUD:${'$'}(/data/adb/ksud --version 2>/dev/null)"
echo "MAGISK:${'$'}(magisk -v 2>/dev/null)"
echo "MZ:${'$'}(magisk --sqlite "select value from settings where key='zygisk'" 2>/dev/null)"
echo "MODS:${'$'}(ls /data/adb/modules 2>/dev/null | tr '\n' ' ')"
"""

    /** Blocking. Asks for root once; returns null when root is not granted. */
    fun read(): RootResult? {
        return try {
            val p = ProcessBuilder("su", "-c", SCRIPT).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText()
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroy()
                return null
            }
            if (!out.contains("PKGS:")) return null
            val map = out.lines().filter { it.contains(':') }
                .associate { it.substringBefore(':') to it.substringAfter(':').trim() }
            val pkgs = map["PKGS"].orEmpty()
            val ksud = map["KSUD"].orEmpty()
            val magisk = map["MAGISK"].orEmpty()
            val mods = map["MODS"].orEmpty()
            val solution = when {
                pkgs.contains("ksunext") -> "KernelSU Next" + if (ksud.isNotBlank()) " ($ksud)" else ""
                pkgs.contains("me.weishu.kernelsu") -> "KernelSU" + if (ksud.isNotBlank()) " ($ksud)" else ""
                pkgs.contains("me.bmax.apatch") -> "APatch"
                pkgs.contains("topjohnwu.magisk") || magisk.isNotBlank() -> "Magisk $magisk".trim()
                ksud.isNotBlank() -> "KernelSU ($ksud)"
                else -> "Root"
            }
            val zygisk = when {
                mods.contains("zygisksu") -> "Zygisk Next"
                mods.contains("rezygisk") -> "ReZygisk"
                map["MZ"] == "1" -> "Magisk Zygisk"
                mods.contains("zygisk_lsposed") -> "Zygisk"
                else -> ""
            }
            RootResult(solution, zygisk)
        } catch (t: Throwable) {
            null
        }
    }
}
