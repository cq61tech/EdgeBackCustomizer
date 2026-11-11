package com.edgeback.customizer

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class StatusColors(val container: Color, val onContainer: Color)

fun greenFor(primary: Color, dark: Boolean): StatusColors {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
    val target = 140f
    val diff = ((hsv[0] - target + 540f) % 360f) - 180f
    val hue = target + diff.coerceIn(-18f, 18f)
    return if (dark) {
        StatusColors(Color.hsv(hue, 0.45f, 0.30f), Color.hsv(hue, 0.20f, 0.95f))
    } else {
        StatusColors(Color.hsv(hue, 0.22f, 0.96f), Color.hsv(hue, 0.80f, 0.20f))
    }
}

private fun hsl(h: Float, s: Float, l: Float): Color =
    Color.hsl(((h % 360f) + 360f) % 360f, s, l)

fun hslColor(h: Float, s: Float, l: Float): Color =
    Color.hsl(((h % 360f) + 360f) % 360f, (s / 100f).coerceIn(0f, 1f), (l / 100f).coerceIn(0f, 1f))

private fun accentScheme(hue: Float, dark: Boolean, amoled: Boolean): ColorScheme {
    val sh = hue + 20f
    val sec = hue + 15f
    val ter = hue + 60f
    return if (dark) {
        val bg = if (amoled) Color.Black else hsl(sh, 0.10f, 0.08f)
        darkColorScheme(
            primary = hsl(hue, 0.78f, 0.73f),
            onPrimary = hsl(hue, 0.65f, 0.19f),
            primaryContainer = hsl(sec, 0.22f, 0.27f),
            onPrimaryContainer = hsl(sec, 0.60f, 0.90f),
            secondary = hsl(sec, 0.35f, 0.75f),
            onSecondary = hsl(sec, 0.30f, 0.20f),
            secondaryContainer = hsl(sec, 0.20f, 0.28f),
            onSecondaryContainer = hsl(sec, 0.50f, 0.90f),
            tertiary = hsl(ter, 0.40f, 0.75f),
            onTertiary = hsl(ter, 0.30f, 0.20f),
            tertiaryContainer = hsl(ter, 0.25f, 0.28f),
            onTertiaryContainer = hsl(ter, 0.50f, 0.90f),
            background = bg,
            onBackground = hsl(sh, 0.10f, 0.90f),
            surface = bg,
            onSurface = hsl(sh, 0.10f, 0.90f),
            surfaceVariant = hsl(sh, 0.08f, 0.28f),
            onSurfaceVariant = hsl(sh, 0.10f, 0.76f),
            outline = hsl(sh, 0.07f, 0.58f),
            outlineVariant = hsl(sh, 0.08f, 0.30f),
            surfaceContainerLowest = if (amoled) Color.Black else hsl(sh, 0.09f, 0.06f),
            surfaceContainerLow = if (amoled) Color(0xFF0A0A0A) else hsl(sh, 0.08f, 0.11f),
            surfaceContainer = if (amoled) Color(0xFF121212) else hsl(sh, 0.07f, 0.13f),
            surfaceContainerHigh = if (amoled) Color(0xFF1A1A1A) else hsl(sh, 0.07f, 0.16f),
            surfaceContainerHighest = if (amoled) Color(0xFF222222) else hsl(sh, 0.07f, 0.19f),
        )
    } else {
        lightColorScheme(
            primary = hsl(hue, 0.50f, 0.45f),
            onPrimary = Color.White,
            primaryContainer = hsl(sec, 0.80f, 0.90f),
            onPrimaryContainer = hsl(hue, 0.60f, 0.20f),
            secondary = hsl(sec, 0.25f, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hsl(sec, 0.55f, 0.90f),
            onSecondaryContainer = hsl(sec, 0.45f, 0.18f),
            tertiary = hsl(ter, 0.30f, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hsl(ter, 0.55f, 0.90f),
            onTertiaryContainer = hsl(ter, 0.45f, 0.18f),
            background = hsl(sh, 0.25f, 0.98f),
            onBackground = hsl(sh, 0.15f, 0.10f),
            surface = hsl(sh, 0.25f, 0.98f),
            onSurface = hsl(sh, 0.15f, 0.10f),
            surfaceVariant = hsl(sh, 0.25f, 0.90f),
            onSurfaceVariant = hsl(sh, 0.10f, 0.30f),
            outline = hsl(sh, 0.06f, 0.46f),
            outlineVariant = hsl(sh, 0.15f, 0.80f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hsl(sh, 0.25f, 0.96f),
            surfaceContainer = hsl(sh, 0.22f, 0.94f),
            surfaceContainerHigh = hsl(sh, 0.20f, 0.92f),
            surfaceContainerHighest = hsl(sh, 0.18f, 0.90f),
        )
    }
}

@Composable
fun EbcTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val dark = when (prefs.themeMode.value) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    val amoled = dark && prefs.amoled.value
    val scheme = if (prefs.monet.value && Build.VERSION.SDK_INT >= 31) {
        val base = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        if (amoled) {
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainer = Color(0xFF121212),
                surfaceContainerHigh = Color(0xFF1A1A1A),
                surfaceContainerHighest = Color(0xFF222222),
            )
        } else {
            base
        }
    } else {
        accentScheme(prefs.accentHue.value, dark, amoled)
    }
    val typography = remember(
        prefs.fontId.value, prefs.fontWeight.value, prefs.fontRound.value,
        prefs.fontScale.value, prefs.fontSpacing.value,
    ) {
        AppFonts.typography(
            ctx, prefs.fontId.value, prefs.fontWeight.value, prefs.fontRound.value,
            prefs.fontScale.value, prefs.fontSpacing.value,
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp),
        ),
        content = content,
    )
}
