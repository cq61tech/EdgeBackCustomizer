package com.edgeback.customizer.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgeback.customizer.ModuleStatus
import com.edgeback.customizer.Prefs
import com.edgeback.customizer.R
import com.edgeback.customizer.RootInfo
import com.edgeback.customizer.greenFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REPO_URL = "https://github.com/cq61tech/EdgeBackCustomizer"

@Composable
fun HomeScreen(prefs: Prefs) {
    var showInfo by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val restart = rememberRestartSystemUi()

    val readRoot: () -> Unit = {
        if (!reading) {
            reading = true
            scope.launch {
                val r = withContext(Dispatchers.IO) { RootInfo.read() }
                prefs.rootSolution.value = r?.solution.orEmpty()
                prefs.zygisk.value = r?.zygisk.orEmpty()
                prefs.rootChecked.value = true
                reading = false
            }
        }
    }

    ScreenColumn {
        Hero()
        StatusCard(tick = tick, onRestart = restart, onRefresh = { tick++ })
        DeviceCard(prefs, reading, readRoot)
        WhatCanCard { showInfo = true }
        Tiles(prefs)
        CreditsCard()
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.close)) }
            },
            title = { Text(stringResource(R.string.whatcan_title)) },
            text = { Text(stringResource(R.string.whatcan_body)) },
        )
    }
}

@Composable
private fun Hero() {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(36.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.header_sky),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.25f to Color.Transparent,
                        1f to Color(0xD9000E1E),
                    ),
                ),
        )
        Text(
            text = stringResource(R.string.quote),
            color = Color.White,
            textAlign = TextAlign.Start,
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 25.sp,
                lineHeight = 33.sp,
                shadow = Shadow(Color(0x99000000), Offset(0f, 2f), 6f),
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 22.dp),
        )
    }
}

@Composable
private fun StatusCard(tick: Int, onRestart: () -> Unit, onRefresh: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dark = cs.background.luminance() < 0.5f
    val on = ModuleStatus.active // re-read on every refresh (tick changes recompose this)
    val green = greenFor(cs.primary, dark)
    val container = if (on) green.container else cs.errorContainer
    val content = if (on) green.onContainer else cs.onErrorContainer

    val lsposedValue = if (on) {
        stringResource(
            R.string.status_active,
            ModuleStatus.frameworkName,
            ModuleStatus.frameworkVersion,
            ModuleStatus.apiVersion,
        )
    } else {
        stringResource(R.string.status_inactive)
    }
    val hooksValue = if (on) stringResource(R.string.hooks_discovery) else stringResource(R.string.status_inactive)

    Surface(shape = CardShape, color = container, contentColor = content, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(
                    if (on) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Text(stringResource(R.string.status_title), style = MaterialTheme.typography.headlineSmall)
            }
            StatusRow(stringResource(R.string.row_lsposed), lsposedValue, content)
            StatusRow(stringResource(R.string.row_hooks), hooksValue, content)
            Text(
                if (on) stringResource(R.string.status_on_note) else stringResource(R.string.status_off_sub),
                style = MaterialTheme.typography.bodyLarge,
            )
            val border = BorderStroke(1.dp, content.copy(alpha = 0.55f))
            val colors = ButtonDefaults.outlinedButtonColors(contentColor = content)
            OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth(), border = border, colors = colors) {
                Text(stringResource(R.string.btn_restart_systemui))
            }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth(), border = border, colors = colors) {
                Text(stringResource(R.string.btn_refresh))
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, content: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = content.copy(alpha = 0.75f))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun DeviceCard(prefs: Prefs, reading: Boolean, onReadRoot: () -> Unit) {
    val unknown = stringResource(R.string.unknown)
    val tap = stringResource(R.string.tap_to_read)
    val readingText = stringResource(R.string.reading)
    val noRoot = stringResource(R.string.no_root)

    val rootText = when {
        reading -> readingText
        !prefs.rootChecked.value -> tap
        prefs.rootSolution.value.isBlank() -> noRoot
        else -> prefs.rootSolution.value
    }
    val zygiskText = when {
        reading -> readingText
        !prefs.rootChecked.value -> tap
        prefs.zygisk.value.isBlank() -> unknown
        else -> prefs.zygisk.value
    }
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }

    SectionCard(stringResource(R.string.device_title)) {
        InfoRow(stringResource(R.string.row_device), "$manufacturer ${Build.MODEL}")
        InfoRow(stringResource(R.string.row_codename), Build.DEVICE)
        InfoRow(stringResource(R.string.row_android), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        InfoRow(stringResource(R.string.row_zygisk), zygiskText, onReadRoot)
        InfoRow(stringResource(R.string.row_root), rootText, onReadRoot)
    }
}

@Composable
private fun WhatCanCard(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = CardShape,
        color = cs.primaryContainer,
        contentColor = cs.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().clip(CardShape).clickable { onClick() },
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(cs.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = cs.onPrimary)
            }
            Column {
                Text(stringResource(R.string.whatcan_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.whatcan_summary), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Tiles(prefs: Prefs) {
    val theme = when (prefs.themeMode.value) {
        1 -> stringResource(R.string.theme_light)
        2 -> stringResource(R.string.theme_dark)
        else -> stringResource(R.string.theme_system)
    }
    val monet = if (prefs.monet.value) stringResource(R.string.on) else stringResource(R.string.off)
    val dock = if (prefs.dockFloating.value) {
        stringResource(R.string.dock_floating_value)
    } else {
        stringResource(R.string.dock_classic_value)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile(stringResource(R.string.tile_theme), theme, Modifier.weight(1f))
        Tile(stringResource(R.string.tile_monet), monet, Modifier.weight(1f))
        Tile(stringResource(R.string.tile_dock), dock, Modifier.weight(1f))
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
    }
}

@Composable
private fun CreditsCard() {
    val uri = LocalUriHandler.current
    SectionCard(stringResource(R.string.credits_title)) {
        Text(stringResource(R.string.credits_body), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { uri.openUri(REPO_URL) }) {
            Text(stringResource(R.string.repo_button))
        }
    }
}
