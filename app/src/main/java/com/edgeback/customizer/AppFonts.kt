package com.edgeback.customizer

import android.content.Context
import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import java.io.File
import kotlin.math.roundToInt

object AppFonts {
    class Option(val id: String, val label: String)
    private class Resolved(val family: FontFamily?, val typefaceBased: Boolean)

    private val cache = HashMap<String, Typeface?>()

    fun clearCache() = cache.clear()

    fun gsfPath(): String? {
        val rx = Regex("(?i).*google.?sans.?flex.*\\.(ttf|otf)")
        for (d in listOf("/system/fonts", "/system_ext/fonts", "/product/fonts", "/vendor/fonts")) {
            val files = File(d).listFiles() ?: continue
            val hit = files.filter { rx.matches(it.name) && !it.name.contains("Italic", true) }
                .minByOrNull { it.name }
            if (hit != null) return hit.absolutePath
        }
        return null
    }

    private fun assetFonts(ctx: Context): List<String> =
        try {
            (ctx.assets.list("fonts") ?: emptyArray()).filter {
                it.endsWith(".ttf", true) || it.endsWith(".otf", true)
            }.sorted()
        } catch (t: Throwable) {
            emptyList()
        }

    private fun importedFonts(ctx: Context): List<String> =
        File(ctx.filesDir, "fonts").listFiles()?.map { it.name }?.sorted() ?: emptyList()

    fun options(ctx: Context, autoLabel: String, defaultLabel: String): List<Option> {
        val list = mutableListOf(Option("auto", autoLabel), Option("default", defaultLabel))
        if (gsfPath() != null) list += Option("gsf", "Google Sans Flex")
        assetFonts(ctx).forEach { list += Option("asset:$it", it.substringBeforeLast('.')) }
        importedFonts(ctx).forEach { list += Option("file:$it", it.substringBeforeLast('.')) }
        list += Option("g:sans", "Sans Serif")
        list += Option("g:serif", "Serif")
        list += Option("g:mono", "Monospace")
        list += Option("g:cursive", "Cursive")
        return list
    }

    private fun resolveAuto(ctx: Context): String {
        if (gsfPath() != null) return "gsf"
        val assets = assetFonts(ctx)
        for (name in listOf("Nunito.ttf", "Quicksand.ttf", "Fredoka.ttf", "Outfit.ttf")) {
            if (name in assets) return "asset:$name"
        }
        return "default"
    }

    private fun build(ctx: Context, id: String, weight: Int, round: Int): Typeface? {
        val key = "$id|$weight|$round"
        if (cache.containsKey(key)) return cache[key]
        val settings = "'wght' $weight, 'ROND' $round"
        val tf = try {
            when {
                id == "gsf" -> gsfPath()?.let {
                    Typeface.Builder(File(it)).setFontVariationSettings(settings).build()
                }
                id.startsWith("asset:") ->
                    Typeface.Builder(ctx.assets, "fonts/" + id.removePrefix("asset:"))
                        .setFontVariationSettings(settings).build()
                id.startsWith("file:") ->
                    Typeface.Builder(File(File(ctx.filesDir, "fonts"), id.removePrefix("file:")))
                        .setFontVariationSettings(settings).build()
                else -> null
            }
        } catch (t: Throwable) {
            null
        }
        cache[key] = tf
        return tf
    }

    private fun resolve(ctx: Context, id0: String, weight: Int, round: Int): Resolved {
        val id = if (id0 == "auto") resolveAuto(ctx) else id0
        return when (id) {
            "g:sans" -> Resolved(FontFamily.SansSerif, false)
            "g:serif" -> Resolved(FontFamily.Serif, false)
            "g:mono" -> Resolved(FontFamily.Monospace, false)
            "g:cursive" -> Resolved(FontFamily.Cursive, false)
            "default" -> Resolved(null, false)
            else -> {
                val tf = build(ctx, id, weight, round)
                if (tf != null) Resolved(FontFamily(tf), true) else Resolved(null, false)
            }
        }
    }

    fun typography(
        ctx: Context,
        id: String,
        weight: Float,
        round: Float,
        scale: Float,
        spacing: Float,
    ): Typography {
        val base = Typography()
        val sc = scale / 100f
        fun map(s: TextStyle, offset: Int): TextStyle {
            val w = (weight.roundToInt() + offset).coerceIn(100, 900)
            val r = resolve(ctx, id, w, round.roundToInt())
            return s.copy(
                fontFamily = r.family ?: s.fontFamily,
                fontWeight = if (r.typefaceBased) FontWeight.Normal else FontWeight(w),
                fontSize = s.fontSize * sc,
                lineHeight = s.lineHeight * sc,
                letterSpacing = if (spacing == 0f) s.letterSpacing else (spacing / 1000f).em,
            )
        }
        return base.copy(
            displayLarge = map(base.displayLarge, 150),
            displayMedium = map(base.displayMedium, 150),
            displaySmall = map(base.displaySmall, 150),
            headlineLarge = map(base.headlineLarge, 150),
            headlineMedium = map(base.headlineMedium, 150),
            headlineSmall = map(base.headlineSmall, 150),
            titleLarge = map(base.titleLarge, 100),
            titleMedium = map(base.titleMedium, 100),
            titleSmall = map(base.titleSmall, 100),
            bodyLarge = map(base.bodyLarge, 0),
            bodyMedium = map(base.bodyMedium, 0),
            bodySmall = map(base.bodySmall, 0),
            labelLarge = map(base.labelLarge, 100),
            labelMedium = map(base.labelMedium, 100),
            labelSmall = map(base.labelSmall, 100),
        )
    }
}
