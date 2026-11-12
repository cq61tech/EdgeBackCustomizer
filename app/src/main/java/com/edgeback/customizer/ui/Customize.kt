package com.edgeback.customizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgeback.customizer.FloatPref
import com.edgeback.customizer.Prefs
import com.edgeback.customizer.R
import com.edgeback.customizer.hslColor
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CustomizeScreen(prefs: Prefs) {
    val cs = MaterialTheme.colorScheme
    ScreenColumn {
        Text(
            stringResource(R.string.preview_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        PreviewCard(prefs)
        Text(
            stringResource(R.string.preview_caption),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            stringResource(R.string.preview_tip),
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        SectionCard(stringResource(R.string.card_pill)) {
            SwitchRow(
                stringResource(R.string.look_title),
                stringResource(R.string.look_summary),
                prefs.pillLook.value,
            ) { prefs.pillLook.value = it }
            SwitchRow(
                stringResource(R.string.custom_colors_title),
                stringResource(R.string.custom_colors_summary),
                prefs.pillColorCustom.value,
            ) { prefs.pillColorCustom.value = it }
            if (prefs.pillColorCustom.value) {
                ColorEditor(stringResource(R.string.color_bg), prefs.bgH, prefs.bgS, prefs.bgL)
                ColorEditor(stringResource(R.string.color_arrow), prefs.fgH, prefs.fgS, prefs.fgL)
            }
            SwitchRow(
                stringResource(R.string.gradient_title),
                stringResource(R.string.gradient_summary),
                prefs.pillGradient.value,
            ) { prefs.pillGradient.value = it }
            if (prefs.pillGradient.value) {
                if (prefs.pillColorCustom.value) {
                    ColorEditor(stringResource(R.string.color_grad), prefs.g2H, prefs.g2S, prefs.g2L)
                }
                SliderRow(stringResource(R.string.grad_angle), prefs.gradAngle.value, 0f..360f, "°") {
                    prefs.gradAngle.value = it
                }
            }
            SliderRow(stringResource(R.string.opt_alpha), prefs.pillAlpha.value, 20f..100f, "%") {
                prefs.pillAlpha.value = it
            }
            SliderRow(stringResource(R.string.opt_glow), prefs.glow.value, 0f..40f, " dp") {
                prefs.glow.value = it
            }
        }

        SectionCard(stringResource(R.string.card_size)) {
            SliderRow(stringResource(R.string.opt_scale), prefs.pillScale.value, 0.4f..2.5f, "x", decimals = 2) {
                prefs.pillScale.value = it
            }
            SliderRow(stringResource(R.string.opt_width), prefs.pillW.value, 40f..250f, "%") {
                prefs.pillW.value = it
            }
            SliderRow(stringResource(R.string.opt_height), prefs.pillH.value, 40f..250f, "%") {
                prefs.pillH.value = it
            }
            SliderRow(stringResource(R.string.opt_radius), prefs.pillRound.value, 0f..100f, "%") {
                prefs.pillRound.value = it
            }
            SliderRow(stringResource(R.string.opt_arrow_size), prefs.arrowSize.value, 40f..250f, "%") {
                prefs.arrowSize.value = it
            }
            SliderRow(stringResource(R.string.opt_arrow_stroke), prefs.arrowStroke.value, 0f..12f, " dp", decimals = 1) {
                prefs.arrowStroke.value = it
            }
            SliderRow(stringResource(R.string.opt_outline), prefs.outline.value, 0f..8f, " dp", decimals = 1) {
                prefs.outline.value = it
            }
            SliderRow(stringResource(R.string.opt_offset), prefs.offsetY.value, -200f..200f, " dp") {
                prefs.offsetY.value = it
            }
        }

        SectionCard(stringResource(R.string.card_behavior)) {
            SliderRow(stringResource(R.string.opt_zone), prefs.zonePercent.value, 20f..100f, "%") {
                prefs.zonePercent.value = it
            }
            SliderRow(stringResource(R.string.opt_edge_width), prefs.edgeWidth.value, 40f..300f, "%") {
                prefs.edgeWidth.value = it
            }
            SwitchRow(stringResource(R.string.opt_edge_left), null, prefs.edgeLeft.value) { prefs.edgeLeft.value = it }
            SwitchRow(stringResource(R.string.opt_edge_right), null, prefs.edgeRight.value) { prefs.edgeRight.value = it }
            SwitchRow(stringResource(R.string.opt_haptic), null, prefs.haptic.value) { prefs.haptic.value = it }
        }

        Text(
            stringResource(R.string.customize_note),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun ColorEditor(title: String, h: FloatPref, s: FloatPref, l: FloatPref) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(hslColor(h.value, s.value, l.value)))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        SliderRow(stringResource(R.string.hue), h.value, 0f..360f, "°") { h.value = it }
        SliderRow(stringResource(R.string.saturation), s.value, 0f..100f, "%") { s.value = it }
        SliderRow(stringResource(R.string.lightness), l.value, 0f..100f, "%") { l.value = it }
    }
}

@Composable
private fun PreviewCard(prefs: Prefs) {
    val cs = MaterialTheme.colorScheme
    val custom = prefs.pillColorCustom.value
    val c1 = if (custom) hslColor(prefs.bgH.value, prefs.bgS.value, prefs.bgL.value) else cs.primary
    val c2 = if (custom) hslColor(prefs.g2H.value, prefs.g2S.value, prefs.g2L.value) else cs.tertiary
    val arrowC = if (custom) hslColor(prefs.fgH.value, prefs.fgS.value, prefs.fgL.value) else cs.onPrimary

    Surface(
        shape = CardShape,
        color = cs.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth().height(200.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val d = this.density
            if (prefs.edgeLeft.value) {
                val zoneH = size.height * (prefs.zonePercent.value / 100f)
                drawRoundRect(
                    color = cs.tertiary.copy(alpha = 0.35f),
                    topLeft = Offset(0f, size.height - zoneH),
                    size = Size(5f * d, zoneH),
                    cornerRadius = CornerRadius(3f * d, 3f * d),
                )
            }
            val sc = prefs.pillScale.value
            val bw = 76f * d * sc * prefs.pillW.value / 100f
            val bh = 76f * d * sc * prefs.pillH.value / 100f
            val cx = size.width / 2f
            val cy = size.height / 2f + prefs.offsetY.value * d
            val rad = min(bw, bh) / 2f * (prefs.pillRound.value / 100f)

            if (prefs.glow.value > 0f) {
                val g = prefs.glow.value * d
                drawRoundRect(
                    color = c1.copy(alpha = 0.22f),
                    topLeft = Offset(cx - bw / 2f - g, cy - bh / 2f - g),
                    size = Size(bw + 2f * g, bh + 2f * g),
                    cornerRadius = CornerRadius(rad + g, rad + g),
                )
            }
            val brush = if (prefs.pillGradient.value) {
                val a = Math.toRadians(prefs.gradAngle.value.toDouble())
                val dx = (cos(a) * bw / 2f).toFloat()
                val dy = (sin(a) * bh / 2f).toFloat()
                Brush.linearGradient(listOf(c1, c2), start = Offset(cx - dx, cy - dy), end = Offset(cx + dx, cy + dy))
            } else {
                SolidColor(c1)
            }
            drawRoundRect(
                brush = brush,
                topLeft = Offset(cx - bw / 2f, cy - bh / 2f),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(rad, rad),
                alpha = prefs.pillAlpha.value / 100f,
            )
            if (prefs.outline.value > 0f) {
                val ow = prefs.outline.value * d
                drawRoundRect(
                    color = arrowC,
                    topLeft = Offset(cx - bw / 2f + ow / 2f, cy - bh / 2f + ow / 2f),
                    size = Size(bw - ow, bh - ow),
                    cornerRadius = CornerRadius(maxOf(0f, rad - ow / 2f), maxOf(0f, rad - ow / 2f)),
                    style = Stroke(width = ow),
                    alpha = prefs.pillAlpha.value / 100f,
                )
            }
            val k = prefs.arrowSize.value / 100f
            val ax = min(bw, bh) * 0.11f * k
            val ay = min(bw, bh) * 0.20f * k
            val arrow = Path().apply {
                moveTo(cx + ax, cy - ay)
                lineTo(cx - ax, cy)
                lineTo(cx + ax, cy + ay)
            }
            val sw = (if (prefs.arrowStroke.value > 0f) prefs.arrowStroke.value else 2.5f) * d
            drawPath(
                arrow,
                color = arrowC,
                style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
