package com.edgeback.customizer.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.edgeback.customizer.ModuleStatus
import com.edgeback.customizer.Prefs
import com.edgeback.customizer.R
import com.edgeback.customizer.setLauncherIconHidden

private class Entry(val keywords: String, val content: @Composable () -> Unit)

private fun currentLanguageTag(): String {
    val l = AppCompatDelegate.getApplicationLocales()
    return if (l.isEmpty()) "" else (l.get(0)?.language ?: "")
}

@Composable
fun SettingsScreen(prefs: Prefs) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val restart = rememberRestartSystemUi()

    val tLang = stringResource(R.string.language_title)
    val tTheme = stringResource(R.string.theme_title)
    val tMonet = stringResource(R.string.monet_title)
    val tAccent = stringResource(R.string.accent_title)
    val tAmoled = stringResource(R.string.amoled_title)
    val tBottom = stringResource(R.string.card_bottom)
    val tDock = stringResource(R.string.dock_floating_title)
    val tModule = stringResource(R.string.card_module)
    val tHooks = stringResource(R.string.hooks_enable_title)
    val tDebug = stringResource(R.string.debug_title)
    val tHide = stringResource(R.string.hide_icon_title)
    val tDiag = stringResource(R.string.card_diag)
    val tBackup = stringResource(R.string.card_backup)
    val tExported = stringResource(R.string.toast_exported)
    val tImported = stringResource(R.string.toast_imported)
    val tFailed = stringResource(R.string.toast_failed)
    val unknown = stringResource(R.string.unknown)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(prefs.exportJson().toByteArray()) }
            }.isSuccess
            Toast.makeText(ctx, if (ok) tExported else tFailed, Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                prefs.importJson(text)
            }.getOrDefault(false)
            Toast.makeText(ctx, if (ok) tImported else tFailed, Toast.LENGTH_SHORT).show()
        }
    }

    val langTag = currentLanguageTag()
    val entries = listOf(
        Entry(tLang) {
            SectionCard(tLang) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val options = listOf(
                        "" to R.string.language_system,
                        "en" to R.string.language_en,
                        "tr" to R.string.language_tr,
                    )
                    options.forEach { (tag, res) ->
                        FilterChip(
                            selected = langTag == tag,
                            onClick = {
                                AppCompatDelegate.setApplicationLocales(
                                    if (tag.isEmpty()) {
                                        LocaleListCompat.getEmptyLocaleList()
                                    } else {
                                        LocaleListCompat.forLanguageTags(tag)
                                    },
                                )
                            },
                            label = { Text(stringResource(res)) },
                        )
                    }
                }
            }
        },
        Entry("$tTheme $tMonet $tAccent $tAmoled") {
            SectionCard(tTheme) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val labels = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
                    labels.forEachIndexed { i, res ->
                        SegmentedButton(
                            selected = prefs.themeMode.value == i,
                            onClick = { prefs.themeMode.value = i },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = labels.size),
                            label = { Text(stringResource(res)) },
                        )
                    }
                }
                SwitchRow(
                    tMonet,
                    stringResource(R.string.monet_summary),
                    prefs.monet.value,
                    enabled = Build.VERSION.SDK_INT >= 31,
                ) { prefs.monet.value = it }
                SliderRow(tAccent, prefs.accentHue.value, 0f..360f, "°", enabled = !prefs.monet.value) {
                    prefs.accentHue.value = it
                }
                SwitchRow(tAmoled, null, prefs.amoled.value) { prefs.amoled.value = it }
            }
        },
        Entry("$tBottom $tDock") {
            SectionCard(tBottom) {
                SwitchRow(tDock, stringResource(R.string.dock_floating_summary), prefs.dockFloating.value) {
                    prefs.dockFloating.value = it
                }
                SliderRow(stringResource(R.string.dock_width), prefs.dockWidth.value, 30f..100f, "%",
                    enabled = prefs.dockFloating.value) { prefs.dockWidth.value = it }
                SliderRow(stringResource(R.string.dock_height), prefs.dockHeight.value, 48f..96f, " dp") {
                    prefs.dockHeight.value = it
                }
                SliderRow(stringResource(R.string.dock_opacity), prefs.dockOpacity.value, 30f..100f, "%") {
                    prefs.dockOpacity.value = it
                }
                SliderRow(stringResource(R.string.dock_text), prefs.dockTextSize.value, 8f..16f, " sp") {
                    prefs.dockTextSize.value = it
                }
                SliderRow(stringResource(R.string.dock_bottom), prefs.dockBottom.value, 0f..48f, " dp",
                    enabled = prefs.dockFloating.value) { prefs.dockBottom.value = it }
                SwitchRow(
                    stringResource(R.string.dock_hide_title),
                    stringResource(R.string.dock_hide_summary),
                    prefs.dockHide.value,
                ) { prefs.dockHide.value = it }
                SwitchRow(stringResource(R.string.dock_labels_title), null, prefs.dockLabels.value) {
                    prefs.dockLabels.value = it
                }
            }
        },
        Entry("$tModule $tHooks $tDebug $tHide") {
            SectionCard(tModule) {
                SwitchRow(tHooks, stringResource(R.string.hooks_enable_summary), prefs.enableHooks.value) {
                    prefs.enableHooks.value = it
                }
                SwitchRow(tDebug, stringResource(R.string.debug_summary), prefs.debugLog.value) {
                    prefs.debugLog.value = it
                }
                SwitchRow(tHide, stringResource(R.string.hide_icon_summary), prefs.hideIcon.value) {
                    prefs.hideIcon.value = it
                    setLauncherIconHidden(ctx, it)
                }
                WideOutlinedButton(stringResource(R.string.btn_restart_systemui)) { restart() }
            }
        },
        Entry(tDiag) {
            SectionCard(tDiag) {
                InfoRow(stringResource(R.string.row_android), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                InfoRow(stringResource(R.string.diag_model), "${Build.MANUFACTURER} ${Build.MODEL}")
                InfoRow(stringResource(R.string.diag_arch), Build.SUPPORTED_ABIS.firstOrNull() ?: unknown)
                InfoRow(
                    stringResource(R.string.diag_framework),
                    if (ModuleStatus.active) "${ModuleStatus.frameworkName} ${ModuleStatus.frameworkVersion}" else unknown,
                )
                InfoRow(
                    stringResource(R.string.diag_module),
                    if (ModuleStatus.active) stringResource(R.string.hooks_discovery) else stringResource(R.string.status_inactive),
                )
            }
        },
        Entry(tBackup) {
            SectionCard(tBackup) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WideOutlinedButton(stringResource(R.string.export_btn), Modifier.weight(1f)) {
                        exportLauncher.launch("edgeback-settings.json")
                    }
                    WideOutlinedButton(stringResource(R.string.import_btn), Modifier.weight(1f)) {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                }
            }
        },
    )

    val q = query.trim().lowercase()
    val shown = entries.filter { q.isEmpty() || it.keywords.lowercase().contains(q) }

    ScreenColumn {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (shown.isEmpty()) {
            Text(
                stringResource(R.string.no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        shown.forEach { it.content() }
    }
}
