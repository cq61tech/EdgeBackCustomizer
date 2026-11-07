package com.edgeback.customizer.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.edgeback.customizer.R

private class DockItem(val icon: ImageVector, val labelRes: Int)

private val items = listOf(
    DockItem(Icons.Filled.Home, R.string.nav_home),
    DockItem(Icons.Filled.Edit, R.string.nav_customize),
    DockItem(Icons.Filled.Settings, R.string.nav_settings),
)

@Composable
fun Dock(selected: Int, onSelect: (Int) -> Unit, floating: Boolean, labels: Boolean) {
    val cs = MaterialTheme.colorScheme
    if (!floating) {
        NavigationBar {
            items.forEachIndexed { i, item ->
                NavigationBarItem(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(stringResource(item.labelRes)) },
                    alwaysShowLabel = labels,
                )
            }
        }
    } else {
        Box(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = cs.surfaceContainerHigh,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
            ) {
                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEachIndexed { i, item ->
                        val sel = selected == i
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(if (sel) cs.secondaryContainer else Color.Transparent)
                                .clickable { onSelect(i) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .animateContentSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = if (sel) cs.onSecondaryContainer else cs.onSurfaceVariant,
                            )
                            if (sel || labels) {
                                Text(
                                    stringResource(item.labelRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (sel) cs.onSecondaryContainer else cs.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
