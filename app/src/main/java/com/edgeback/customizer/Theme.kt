package com.edgeback.customizer

import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Uses the system "google-sans-flex" family when the ROM has it, otherwise the default system font. */
val QuoteFont: FontFamily =
    FontFamily(Typeface.create(Typeface.create("google-sans-flex", Typeface.NORMAL), 500, false))

class StatusColors(val container: Color, val onContainer: Color)

/** A green container whose hue is pulled slightly toward the Monet primary, so it sits well in the palette. */
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

@Composable
fun EbcTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val dark = when (prefs.themeMode.value) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    val scheme = if (prefs.monet.value && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else if (dark) {
        darkColorScheme(primary = Color(0xFF9CCAFF), onPrimary = Color(0xFF00325A))
    } else {
        lightColorScheme(primary = Color(0xFF1F6FB2), onPrimary = Color.White)
    }
    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp),
        ),
        content = content,
    )
}
