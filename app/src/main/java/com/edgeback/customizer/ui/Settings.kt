package com.edgeback.customizer.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.edgeback.customizer.Prefs
import com.edgeback.customizer.R

private class Entry(val keywords: String, val content: @Composable () -> Unit)

@Composable
fun SettingsScreen(prefs: Prefs) {
    var query by remember { mutableStateOf("") }
    var showLang by remember { mutableStateOf(false) }

    val tLang = stringResource(R.string.language_title)
    val tTheme = stringResource(R.string.theme_title)
    val tMonet = stringResource(R.string.monet_title)
    val sMonet = stringResource(R.string.monet_summary)
    val tDock = stringResource(R.string.dock_floating_title)
    val sDock = stringResource(R.string.dock_floating_summary)
    val tLabels = stringResource(R.string.dock_labels_title)
    val sLabels = stringResource(R.string.dock_labels_summary)

    val langTag = currentLanguageTag()
    val langLabel = languageLabel(langTag)

    val entries = listOf(
        Entry("$tLang $langLabel") {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().clickable { showLang = true },
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(tLang, style = MaterialTheme.typography.titleMedium)
                    Text(langLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        Entry("$tTheme ${stringResource(R.string.theme_system)} ${stringResource(R.string.theme_light)} ${stringResource(R.string.theme_dark)}") {
            SettingCard {
                Text(tTheme, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val labels = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
                    labels.forEachIndexed { i, res ->
                        FilterChip(
                            selected = prefs.themeMode.value == i,
                            onClick = { prefs.themeMode.value = i },
                            label = { Text(stringResource(res)) },
                        )
                    }
                }
            }
        },
        Entry("$tMonet $sMonet") {
            SwitchCard(tMonet, sMonet, prefs.monet.value) { prefs.monet.value = it }
        },
        Entry("$tDock $sDock") {
            SwitchCard(tDock, sDock, prefs.dockFloating.value) { prefs.dockFloating.value = it }
        },
        Entry("$tLabels $sLabels") {
            SwitchCard(tLabels, sLabels, prefs.dockLabels.value) { prefs.dockLabels.value = it }
        },
    )

    val q = query.trim().lowercase()
    val shown = entries.filter { q.isEmpty() || it.keywords.lowercase().contains(q) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
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

    if (showLang) {
        val options = listOf("" to R.string.language_system, "tr" to R.string.language_tr, "en" to R.string.language_en)
        AlertDialog(
            onDismissRequest = { showLang = false },
            confirmButton = {
                TextButton(onClick = { showLang = false }) { Text(stringResource(R.string.close)) }
            },
            title = { Text(tLang) },
            text = {
                Column {
                    options.forEach { (tag, res) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppCompatDelegate.setApplicationLocales(
                                        if (tag.isEmpty()) {
                                            LocaleListCompat.getEmptyLocaleList()
                                        } else {
                                            LocaleListCompat.forLanguageTags(tag)
                                        },
                                    )
                                    showLang = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = langTag == tag, onClick = null)
                            Text(stringResource(res), modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp))
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun currentLanguageTag(): String {
    val l = AppCompatDelegate.getApplicationLocales()
    return if (l.isEmpty()) "" else (l.get(0)?.language ?: "")
}

@Composable
private fun languageLabel(tag: String): String = when (tag) {
    "tr" -> stringResource(R.string.language_tr)
    "en" -> stringResource(R.string.language_en)
    else -> stringResource(R.string.language_system)
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun SwitchCard(title: String, summary: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clickable { onChecked(!checked) },
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}
