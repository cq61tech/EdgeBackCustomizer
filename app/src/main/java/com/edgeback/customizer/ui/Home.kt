package com.edgeback.customizer.ui

import android.os.Build
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
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
import com.edgeback.customizer.QuoteFont
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
    val scope = rememberCoroutineScope()

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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Hero()
        StatusCard()
        DeviceCard(prefs, reading, readRoot)
        WhatCanCard { showInfo = true }
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
            .aspectRatio(1.6f)
            .clip(MaterialTheme.shapes.extraLarge),
    ) {
        Image(
            painter = painterResource(R.drawable.header_sky),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // darkening scrim so the white quote stays readable on bright clouds
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.30f to Color.Transparent,
                        1f to Color(0xCC001428),
                    ),
                ),
        )
        Text(
            text = stringResource(R.string.quote),
            color = Color.White,
            textAlign = TextAlign.Start,
            style = TextStyle(
                fontFamily = QuoteFont,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                shadow = Shadow(Color(0x99000000), Offset(0f, 2f), 6f),
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 22.dp, vertical = 20.dp),
        )
    }
}

@Composable
private fun StatusCard() {
    val cs = MaterialTheme.colorScheme
    val dark = cs.background.luminance() < 0.5f
    val on = ModuleStatus.active
    val green = greenFor(cs.primary, dark)
    val container = if (on) green.container else cs.errorContainer
    val content = if (on) green.onContainer else cs.onErrorContainer
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                if (on) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Column {
                Text(
                    stringResource(if (on) R.string.status_on else R.string.status_off),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    if (on) {
                        stringResource(
                            R.string.status_on_sub,
                            ModuleStatus.frameworkName,
                            ModuleStatus.frameworkVersion,
                            ModuleStatus.apiVersion,
                        )
                    } else {
                        stringResource(R.string.status_off_sub)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
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
    val lsposed = if (ModuleStatus.active) {
        "${ModuleStatus.frameworkName} ${ModuleStatus.frameworkVersion} (${ModuleStatus.frameworkVersionCode})"
    } else {
        unknown
    }
    val api = if (ModuleStatus.active) ModuleStatus.apiVersion else unknown
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.device_title), style = MaterialTheme.typography.titleMedium)
            InfoRow(stringResource(R.string.row_device), "$manufacturer ${Build.MODEL}")
            InfoRow(stringResource(R.string.row_codename), Build.DEVICE)
            InfoRow(stringResource(R.string.row_android), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            InfoRow(stringResource(R.string.row_lsposed), lsposed)
            InfoRow(stringResource(R.string.row_lsposed_api), api)
            InfoRow(stringResource(R.string.row_zygisk), zygiskText, onReadRoot)
            InfoRow(stringResource(R.string.row_root), rootText, onReadRoot)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    val base = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(base.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun WhatCanCard(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(cs.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = cs.onPrimary)
            }
            Column {
                Text(stringResource(R.string.whatcan_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.whatcan_summary), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CreditsCard() {
    val uri = LocalUriHandler.current
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.credits_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.credits_body), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { uri.openUri(REPO_URL) }) {
                Text(stringResource(R.string.repo_button))
            }
        }
    }
}
