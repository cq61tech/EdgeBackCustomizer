package com.edgeback.customizer

import java.util.concurrent.TimeUnit

class RootResult(val solution: String, val zygisk: String)

object RootInfo {
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
