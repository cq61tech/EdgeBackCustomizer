package com.edgeback.customizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.sp
import com.edgeback.customizer.Prefs
import com.edgeback.customizer.R

private class DockItem(val icon: ImageVector, val labelRes: Int)

private val items = listOf(
    DockItem(Icons.Filled.Home, R.string.nav_home),
    DockItem(Icons.Filled.Edit, R.string.nav_customize),
    DockItem(Icons.Filled.Settings, R.string.nav_settings),
)

/** Bottom bar. Floating pill by default; width, height, opacity, text size and gap come from settings. */
@Composable
fun Dock(selected: Int, onSelect: (Int) -> Unit, prefs: Prefs) {
    val cs = MaterialTheme.colorScheme
    val floating = prefs.dockFloating.value
    val h = prefs.dockHeight.value.dp
    val shape = if (floating) {
        RoundedCornerShape(h / 2)
    } else {
        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    }
    val gap = if (floating) prefs.dockBottom.value.dp else 0.dp
    val labels = prefs.dockLabels.value

    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = gap),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (floating) prefs.dockWidth.value / 100f else 1f)
                .height(h),
            shape = shape,
            color = cs.surfaceContainerHigh.copy(alpha = prefs.dockOpacity.value / 100f),
            shadowElevation = if (floating) 6.dp else 0.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.forEachIndexed { i, item ->
                    val sel = selected == i
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (sel) cs.secondaryContainer else Color.Transparent)
                            .clickable { onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                item.icon,
                                contentDescription = stringResource(item.labelRes),
                                tint = if (sel) cs.onSecondaryContainer else cs.onSurfaceVariant,
                            )
                            if (labels) {
                                Text(
                                    stringResource(item.labelRes),
                                    fontSize = prefs.dockTextSize.value.sp,
                                    maxLines = 1,
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
