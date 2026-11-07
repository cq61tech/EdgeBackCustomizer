package com.edgeback.customizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import kotlin.math.roundToInt

@Composable
fun CustomizeScreen(prefs: Prefs) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.customize_title), style = MaterialTheme.typography.headlineLarge)
        PreviewCard(prefs)
        Text(
            stringResource(R.string.customize_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SliderRow(
                    stringResource(R.string.opt_alpha),
                    "${(prefs.pillAlpha.value * 100).roundToInt()}%",
                    prefs.pillAlpha.value,
                    0.2f..1f,
                ) { prefs.pillAlpha.value = it }
                SliderRow(
                    stringResource(R.string.opt_radius),
                    "${prefs.pillRadius.value.roundToInt()} dp",
                    prefs.pillRadius.value,
                    0f..40f,
                ) { prefs.pillRadius.value = it }
                SliderRow(
                    stringResource(R.string.opt_scale),
                    "${"%.2f".format(prefs.pillScale.value)}x",
                    prefs.pillScale.value,
                    0.6f..1.6f,
                ) { prefs.pillScale.value = it }
                SliderRow(
                    stringResource(R.string.opt_zone),
                    "${prefs.zonePercent.value.roundToInt()}%",
                    prefs.zonePercent.value,
                    20f..100f,
                ) { prefs.zonePercent.value = it }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.opt_haptic), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = prefs.haptic.value, onCheckedChange = { prefs.haptic.value = it })
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(prefs: Prefs) {
    val cs = MaterialTheme.colorScheme
    val pillColor = cs.primary
    val zoneColor = cs.tertiary
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth().height(220.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val density = this.density
            // active zone strip on the left edge (bottom N% of the height)
            val zoneH = size.height * (prefs.zonePercent.value / 100f)
            drawRoundRect(
                color = zoneColor.copy(alpha = 0.35f),
                topLeft = Offset(0f, size.height - zoneH),
                size = Size(5f * density, zoneH),
                cornerRadius = CornerRadius(3f * density, 3f * density),
            )
            // back pill
            val s = prefs.pillScale.value
            val h = 104f * density * s
            val w = 30f * density * s
            val top = size.height / 2f - h / 2f
            val r = min(prefs.pillRadius.value * density, min(w, h) / 2f)
            drawRoundRect(
                color = pillColor.copy(alpha = prefs.pillAlpha.value),
                topLeft = Offset(8f * density, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(r, r),
            )
            // arrow
            val x0 = 8f * density
            val arrow = Path().apply {
                moveTo(x0 + w * 0.64f, top + h * 0.38f)
                lineTo(x0 + w * 0.36f, top + h * 0.50f)
                lineTo(x0 + w * 0.64f, top + h * 0.62f)
            }
            drawPath(
                arrow,
                color = Color.White,
                style = Stroke(width = 3f * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
