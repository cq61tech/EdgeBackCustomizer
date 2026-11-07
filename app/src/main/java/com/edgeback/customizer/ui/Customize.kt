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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgeback.customizer.Prefs
import com.edgeback.customizer.R
import kotlin.math.min

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
                // back pill
                val s = prefs.pillScale.value
                val h = 96f * d * s
                val w = 30f * d * s
                val x0 = 14f * d
                val top = size.height / 2f - h / 2f
                val r = min(prefs.pillRadius.value * d, min(w, h) / 2f)
                drawRoundRect(
                    color = cs.primary.copy(alpha = prefs.pillAlpha.value / 100f),
                    topLeft = Offset(x0, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(r, r),
                )
                val arrow = Path().apply {
                    moveTo(x0 + w * 0.64f, top + h * 0.38f)
                    lineTo(x0 + w * 0.36f, top + h * 0.50f)
                    lineTo(x0 + w * 0.64f, top + h * 0.62f)
                }
                drawPath(
                    arrow,
                    color = cs.onPrimary,
                    style = Stroke(width = 3f * d, cap = StrokeCap.Round, join = StrokeJoin.Round),
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
            SliderRow(stringResource(R.string.opt_alpha), prefs.pillAlpha.value, 20f..100f, "%") {
                prefs.pillAlpha.value = it
            }
            SliderRow(stringResource(R.string.opt_radius), prefs.pillRadius.value, 0f..40f, " dp") {
                prefs.pillRadius.value = it
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
