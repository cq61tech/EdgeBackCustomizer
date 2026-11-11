package com.edgeback.customizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgeback.customizer.Prefs
import com.edgeback.customizer.R

@Composable
fun CustomizeScreen(prefs: Prefs) {
    val cs = MaterialTheme.colorScheme
    ScreenColumn {
        Text(
            stringResource(R.string.preview_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        Surface(
            shape = CardShape,
            color = cs.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth().height(190.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val d = this.density
                // active zone strip on the left edge (bottom N% of the height)
                val zoneH = size.height * (prefs.zonePercent.value / 100f)
                drawRoundRect(
                    color = cs.tertiary.copy(alpha = 0.35f),
                    topLeft = Offset(0f, size.height - zoneH),
                    size = Size(5f * d, zoneH),
                    cornerRadius = CornerRadius(3f * d, 3f * d),
                )
                // back ball: always exactly centered, round by default
                val sc = prefs.pillScale.value
                val dia = 76f * d * sc
                val cx = size.width / 2f
                val cy = size.height / 2f
                val rad = (dia / 2f) * (prefs.pillRound.value / 100f)
                val brush = if (prefs.pillGradient.value) {
                    Brush.verticalGradient(
                        listOf(cs.primary, cs.tertiary),
                        startY = cy - dia / 2f,
                        endY = cy + dia / 2f,
                    )
                } else {
                    SolidColor(cs.primary)
                }
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(cx - dia / 2f, cy - dia / 2f),
                    size = Size(dia, dia),
                    cornerRadius = CornerRadius(rad, rad),
                    alpha = prefs.pillAlpha.value / 100f,
                )
                val a = dia * 0.11f
                val arrow = Path().apply {
                    moveTo(cx + a, cy - dia * 0.20f)
                    lineTo(cx - a, cy)
                    lineTo(cx + a, cy + dia * 0.20f)
                }
                drawPath(
                    arrow,
                    color = cs.onPrimary,
                    style = Stroke(width = 5f * d * sc, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
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
                stringResource(R.string.gradient_title),
                stringResource(R.string.gradient_summary),
                prefs.pillGradient.value,
            ) { prefs.pillGradient.value = it }
            SliderRow(stringResource(R.string.opt_alpha), prefs.pillAlpha.value, 20f..100f, "%") {
                prefs.pillAlpha.value = it
            }
            SliderRow(stringResource(R.string.opt_radius), prefs.pillRound.value, 0f..100f, "%") {
                prefs.pillRound.value = it
            }
            SliderRow(stringResource(R.string.opt_scale), prefs.pillScale.value, 0.6f..1.6f, "x", decimals = 2) {
                prefs.pillScale.value = it
            }
        }

        SectionCard(stringResource(R.string.card_behavior)) {
            SliderRow(stringResource(R.string.opt_zone), prefs.zonePercent.value, 20f..100f, "%") {
                prefs.zonePercent.value = it
            }
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
